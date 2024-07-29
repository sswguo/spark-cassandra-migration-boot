# Cassandra Migration 

This is an option that we can use to do cassandra migration, using the lib [spark-cassandra-connector](https://github.com/datastax/spark-cassandra-connector). 
It's helpful when we want to migrate cassandra to a new cluster, at least it's an option.

# Prerequisites

To complete this, you need:
- Spark 3.4.0
- Cassandra 3.11.X
- Openshift 4
- JDK 11+

# Build Spark Images

Build spark master and worker, for details of spark, ref the [doc](spark/README.md)
```
docker build -t quay.io/sswguo/spark-master:3.4.0-jdk11-1.2 .
docker push quay.io/sswguo/spark-master:3.4.0-jdk11-1.2

docker build -t quay.io/sswguo/spark-worker:3.4.0-jdk11-1.2 .
docker push quay.io/sswguo/spark-worker:3.4.0-jdk11-1.2
```

# Deploy Spark (Master & Worker) into Openshift

Deploy spark
```
oc login --token=<TOKEN> --server=<CLUSTER>

oc apply -f spark-master-deployment.yaml
oc apply -f spark-master-service.yaml

oc apply -f spark-worker-persistent-volume-claim.yaml
oc apply -f spark-worker-deployment.yaml 
```

_NOTE_: Request the suitable storage based on your business, the default size here is `5Gi`. And resources for workers.

# Build the migration application

Build the migration application, confirm the migration app is in read mode
```
mvn clean package
```
# Running the migration - read

Sync the jar into spark worker
```
cp target/migration-1.0-SNAPSHOT.jar app
oc rsync app/ <spark-worker POD_NAME>:/tmp/
```

Configure the setting for cassandra in `/opt/spark/conf/config.yaml`

```
host: cassandra-cluster
port: 9042
user: cassandra
password: cassandra
tables:
  - keyspace: <KEYSPACE>
    table: <TABLE>
    tempView: <View>
    filter: "creation > '2023-08-08'"
    id: <ID>
```

`tempView`: Custom name of the table  
`filter`: If you want to do migration separately, especially the data is large  
`id`: The identities of the directory which stores the CSV files  

Submit the job in worker node
```
spark-submit \
  --class org.commonjava.migration.App \
  --master spark://spark-master:7077 \
  /tmp/migration-1.0-SNAPSHOT.jar
```

# Optimize 

If you have large data in prod, try to use filters to do the migration, for example we can try to use date to split the data
```
Dataset<Row> sqlDF = spark.sql("SELECT * FROM <tablename> where <date_column> > '2023-08-08'");
```
Or the filters when loading the data
```
.filter("some_column > some_value")
.select("relevant_column1", "relevant_column2");
```

# Running the migration - write

```
oc rsync stage_pathmap_migration/ <spark-worker POD_NAME>:/opt/spark/storage/indy_pathmap_0729/
```

Update the migration app to write mode, and then sync the jar into spark worker, submit the job in worker node as above.