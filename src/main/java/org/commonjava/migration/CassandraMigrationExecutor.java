package org.commonjava.migration;

import org.apache.spark.sql.DataFrameWriter;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.InputStream;


public class CassandraMigrationExecutor
{

    //https://github.com/datastax/spark-cassandra-connector/blob/master/doc/7_java_api.md
    final Logger logger = LoggerFactory.getLogger(getClass());

    public MigrationConfig loadConfig() throws Exception
    {
        Yaml yaml = new Yaml(new Constructor(MigrationConfig.class, new LoaderOptions()));
        InputStream inputStream = new FileInputStream("/opt/spark/conf/config.yaml");
        MigrationConfig config = yaml.load(inputStream);
        logger.info("Cassandra config: {}", config);
        return config;
    }

    private SparkSession initSessions( MigrationConfig config, String appName ) throws Exception
    {
        SparkSession spark = null;
        try
        {
            spark = SparkSession.builder()
                    .appName(appName)
                    .master("spark://spark-master:7077")
                    //https://github.com/datastax/spark-cassandra-connector/blob/master/doc/reference.md#cassandra-connection-parameters
                    .config("spark.cassandra.connection.host", config.getHost())
                    .config("spark.cassandra.connection.port", config.getPort()) // Default port, adjust if necessary
                    .config("spark.cassandra.auth.username", config.getUser())
                    .config("spark.cassandra.auth.password", config.getPassword())
                    .config("spark.cassandra.output.consistency.level", "ONE") // QUORUM
                    //.config("spark.cassandra.output.batch.grouping.key", "replica_set") // default: Partition
                    .config("spark.cassandra.output.batch.size.rows", "500") // 2000 for pathmap
                    .config("spark.cassandra.output.batch.size.bytes", "1048576") // 10485760 (10M) for pathmap
                    .config("spark.cassandra.output.concurrent.writes", "5") // 50 for pathmap
                    .config("spark.cassandra.input.consistency.level", "QUORUM")
                    .config("spark.driver.memory", "1g") // Adjust based on your needs
                    .config("spark.executor.memory", "6g") // Adjust based on your needs
                    .config("spark.executor.cores", "4") // Adjust based on your needs
                    .config("spark.driver.extraJavaOptions", "--illegal-access=permit")
                    .config("spark.executor.extraJavaOptions", "--illegal-access=permit")
                    .config("spark.authenticate", "false")
                    //https://aws.amazon.com/blogs/storage/optimizing-performance-of-apache-spark-workloads-on-amazon-s3/
                    //.config("spark.hadoop.parquet.read.allocation.size", "134217728") //128M
                    //.config("spark.sql.files.maxPartitionBytes", "268435456") //256M
                    .getOrCreate();
        }
        catch (Exception e)
        {
            logger.error("Create spark session failed.", e);
        }
        return spark;
    }

    public void export() throws Exception {

     //   https://spark.apache.org/docs/3.4.0/sql-getting-started.html

        logger.info("Start ....");
        // https://github.com/datastax/spark-cassandra-connector/blob/master/doc/reference.md
        // Step 1: Create Spark session
       // https://spark.apache.org/docs/3.4.0/sql-getting-started.html#running-sql-queries-programmatically

        MigrationConfig config = loadConfig();

        SparkSession spark = initSessions( config, "ExportCassandraData-v1.0" );

        logger.info("Spark session created successfully");

        Thread.sleep(30000);

        for ( CassandraTable table : config.getTables() )
        {
            // Reading from a Cassandra table
            Dataset<Row> df = spark.read()
                    .format("org.apache.spark.sql.cassandra")
                    .option("keyspace", table.getKeyspace())
                    .option("table", table.getTable())
                    .load();
            //.filter("some_column > some_value")
            //.select("relevant_column1", "relevant_column2");

            df.createOrReplaceTempView(table.getTempView());
            Dataset<Row> sqlDF = spark.sql("SELECT * FROM " + table.getTempView() + ( table.getFilter() != null ? " where " + table.getFilter() : ""));
            //sqlDF.show();

            if ( !config.getSharedStorage() )
            {
                // ========= write data to local storage ===========
                DataFrameWriter dataFrameWriter = sqlDF.write();

                logger.info("Write dataset to csv file ....");
                String storageDir = "/opt/spark/storage/" + table.getId() + "_" + System.currentTimeMillis();
                if ( config.getFileFormat().equalsIgnoreCase("csv") )
                {
                    dataFrameWriter.option("header", true).csv(storageDir);
                }
                else
                {
                    dataFrameWriter.option("header", true).parquet(storageDir);
                }
                // ========= write data to csv files ===========
            }
            else
            {
                // ========= write data to S3 ===========
                configureAWS(spark, config);

                // Write DataFrame to S3 as CSV files in a directory
                String bucketPath = "s3a://" + config.getBucketName() + "/indy_migration_test/" + table.getKeyspace() + "/" + table.getTable() + "/";

                if ( config.getFileFormat().equalsIgnoreCase("csv") ) {
                    //CSV
                    sqlDF.write()
                        .format("csv")
                        .option("header", "true")
                        .mode("overwrite") // Overwrite if the directory exists
                        .save(bucketPath);
                }
                else
                {
                    sqlDF.write()
                            .option("header", "true")
                            .mode("overwrite") // Overwrite if the directory exists
                            .parquet(bucketPath);
                }
                // ========= write data to S3 ===========
            }
        }

        // Stop the Spark session
        spark.stop();

    }

    public void importData() throws Exception
    {
        logger.info("Start ....");

        MigrationConfig config = loadConfig();

        SparkSession spark = initSessions( config, "ImportCassandraData-v1.0" );

        logger.info("Spark session created successfully");

        Thread.sleep(30000);

        for ( CassandraTable table : config.getTables() )
        {
            Dataset<Row> df = null;
            if ( !config.getSharedStorage() )
            {
                // ========= load data from local storage ===========
                // Load all CSV files from the directory into a DataFrame
                if ( config.getFileFormat().equalsIgnoreCase("csv") )
                {
                    df = spark.read()
                            .format("csv")
                            .option("header", "true") // Use first line of CSV file as header
                            .option("inferSchema", "true") // Automatically infer data types
                            .load("/opt/spark/storage/" + table.getId() + "/*");
                }
                else
                {
                    df = spark.read()
                            .format("parquet")
                            .option("header", "true") // Use first line of CSV file as header
                            .option("inferSchema", "true") // Automatically infer data types
                            .load("/opt/spark/storage/" + table.getId() + "/*");
                }
                // ========= load data from local storage ===========
            }
            else {
                // ========= load data from S3 ===========
                // Configure AWS S3 credentials and S3 bucket path
                configureAWS(spark, config);

                // Read data from S3 into DataFrame
                String bucketPath = "s3a://" + config.getBucketName() + "/indy_migration_test/" + table.getKeyspace() + "/" + table.getTable() + "/";

                if ( config.getFileFormat().equalsIgnoreCase("csv") )
                {
                    df = spark.read()
                        .format("csv")
                        .option("header", "true")
                        .option("inferSchema", "true")
                        .load(bucketPath);
                }
                else
                {
                    //https://aws.amazon.com/blogs/storage/optimizing-performance-of-apache-spark-workloads-on-amazon-s3/
                    df = spark.read().parquet(bucketPath);
                }
                // ========= load data from S3 ===========
            }
            //df.show();
            // Write the DataFrame to Cassandra
            if ( df != null )
            {
                df.write()
                        .format("org.apache.spark.sql.cassandra")
                        .option("keyspace", table.getKeyspace())
                        .option("table", table.getTable())
                        .mode("append")
                        .save();
            }
            else
            {
                logger.warn("No data.");
            }
        }

        // Stop the Spark session
        spark.stop();
    }


    private void configureAWS(SparkSession spark, MigrationConfig config)
    {
        // Configure AWS S3 credentials and S3 bucket path
        String accessKey = config.getAwsAccessKeyID();
        String secretKey = config.getAwsSecretAccessKey();
        String region = config.getBucketRegion();

        // Set AWS S3 credentials and region in Spark session
        spark.sparkContext().hadoopConfiguration().set("fs.s3a.access.key", accessKey);
        spark.sparkContext().hadoopConfiguration().set("fs.s3a.secret.key", secretKey);
        spark.sparkContext().hadoopConfiguration().set("fs.s3a.endpoint", "s3." + region + ".amazonaws.com");
        spark.sparkContext().hadoopConfiguration().set("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");

        // Test for large files upload
        spark.sparkContext().hadoopConfiguration().set("fs.s3a.multipart.uploads.enabled", "true");
        spark.sparkContext().hadoopConfiguration().set("fs.s3a.multipart.size", "10485760");
        //spark.sparkContext().hadoopConfiguration().set("fs.s3a.block.size", "1048576000");
        //spark.sparkContext().hadoopConfiguration().set("fs.s3a.multipart.size", "256M");
        spark.sparkContext().hadoopConfiguration().set("fs.s3a.block.size", "256M");

        // Try larger one to test download, the previous one is 16
        spark.sparkContext().hadoopConfiguration().set("fs.s3a.threads.max", "16");
        spark.sparkContext().hadoopConfiguration().set("fs.s3a.connection.maximum", "16");

    }

}
