package org.commonjava.migration;

import org.apache.spark.sql.DataFrameWriter;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CassandraMigrationExcutor
{

    //https://github.com/datastax/spark-cassandra-connector/blob/master/doc/7_java_api.md
    final Logger logger = LoggerFactory.getLogger(getClass());

    public void run() throws Exception {

     //   https://spark.apache.org/docs/3.4.0/sql-getting-started.html

        logger.info("Start ....");
        // https://github.com/datastax/spark-cassandra-connector/blob/master/doc/reference.md
        // Step 1: Create Spark session
       // https://spark.apache.org/docs/3.4.0/sql-getting-started.html#running-sql-queries-programmatically

        SparkSession spark = null;
        try
        {
            spark = SparkSession.builder()
                    .appName("CassandraMigration-v1.0")
                    .master("spark://spark-master:7077")
                    .config("spark.cassandra.connection.host", "cassandra-cluster")
                    .config("spark.cassandra.connection.port", "9042") // Default port, adjust if necessary
                    .config("spark.cassandra.auth.username", "cassandra")
                    .config("spark.cassandra.auth.password", "cassandra")
                    //.config("spark.cassandra.output.consistency.level", "")
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
            e.printStackTrace();
            return;
        }

        logger.info("Spark session created successfully");

        Thread.sleep(30000);

        // Reading from a Cassandra table
        Dataset<Row> df = spark.read()
                .format("org.apache.spark.sql.cassandra")
                .option("keyspace", "indystorage")
                .option("table", "pathmap")
                .load();
                //.filter("some_column > some_value")
                //.select("relevant_column1", "relevant_column2");

        df.createOrReplaceTempView("pathmap");
        Dataset<Row> sqlDF = spark.sql("SELECT * FROM pathmap where creation > '2023-08-08'");
        //sqlDF.show();

        DataFrameWriter dataFrameWriter = sqlDF.write();

        logger.info("Write dataset to csv file ....");
        String storageDir = "/opt/spark/storage/indy_pathmap_" + System.currentTimeMillis();
        dataFrameWriter.option("header", true).csv(storageDir);

        // Stop the Spark session
        spark.stop();

    }

}
