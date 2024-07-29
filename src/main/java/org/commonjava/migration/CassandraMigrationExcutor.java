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


public class CassandraMigrationExcutor
{

    //https://github.com/datastax/spark-cassandra-connector/blob/master/doc/7_java_api.md
    final Logger logger = LoggerFactory.getLogger(getClass());


    public CassandraConfig loadConfig() throws Exception
    {
        Yaml yaml = new Yaml(new Constructor(CassandraConfig.class, new LoaderOptions()));
        InputStream inputStream = new FileInputStream("/opt/spark/conf/config.yaml");
        CassandraConfig config = yaml.load(inputStream);
        logger.info("Cassandra config: {}", config);
        return config;
    }

    private SparkSession initSessions( CassandraConfig config ) throws Exception
    {
        SparkSession spark = null;
        try
        {
            spark = SparkSession.builder()
                    .appName("CassandraMigration-v1.0")
                    .master("spark://spark-master:7077")
                    .config("spark.cassandra.connection.host", config.getHost())
                    .config("spark.cassandra.connection.port", config.getPort()) // Default port, adjust if necessary
                    .config("spark.cassandra.auth.username", config.getUser())
                    .config("spark.cassandra.auth.password", config.getPassword())
                    .config("spark.cassandra.output.consistency.level", "QUORUM")
                    .config("spark.driver.memory", "1g") // Adjust based on your needs
                    .config("spark.executor.memory", "1g") // Adjust based on your needs
                    .config("spark.executor.cores", "1") // Adjust based on your needs
                    .config("spark.driver.extraJavaOptions", "--illegal-access=permit")
                    .config("spark.executor.extraJavaOptions", "--illegal-access=permit")
                    .config("spark.authenticate", "false")
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

        CassandraConfig config = loadConfig();

        SparkSession spark = initSessions( config );

        logger.info("Spark session created successfully");

        Thread.sleep(30000);

        for ( Table table : config.getTables() )
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

            DataFrameWriter dataFrameWriter = sqlDF.write();

            logger.info("Write dataset to csv file ....");
            String storageDir = "/opt/spark/storage/" + table.getId() + "_" + System.currentTimeMillis();
            dataFrameWriter.option("header", true).csv(storageDir);
        }

        // Stop the Spark session
        spark.stop();

    }

    public void importData() throws Exception
    {
        logger.info("Start ....");

        CassandraConfig config = loadConfig();

        SparkSession spark = initSessions( config );

        logger.info("Spark session created successfully");

        Thread.sleep(30000);

        for ( Table table : config.getTables() )
        {

            // Load all CSV files from the directory into a DataFrame
            Dataset<Row> df = spark.read()
                    .format("csv")
                    .option("header", "true") // Use first line of CSV file as header
                    .option("inferSchema", "true") // Automatically infer data types
                    .load("/opt/spark/storage/" + table.getId() + "/*");

            // Write the DataFrame to Cassandra
            df.write()
                    .format("org.apache.spark.sql.cassandra")
                    .option("keyspace", table.getKeyspace())
                    .option("table", table.getTable())
                    .mode("append")
                    .save();
        }

        // Stop the Spark session
        spark.stop();
    }

}
