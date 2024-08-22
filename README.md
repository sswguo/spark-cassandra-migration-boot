# Cassandra Migration 

This is an option that we can use to do cassandra migration, using the lib [spark-cassandra-connector](https://github.com/datastax/spark-cassandra-connector). 
It's helpful when we want to migrate cassandra to a new cluster, at least it's an option.

# Prerequisites

To complete this, you need:
- Spark 3.4.0
- Cassandra 3.11.X
- Openshift 4
- JDK 11+
- S3 instance( if you want to use shared storage )

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
# Running the migration - read/export

Sync the jar into spark worker
```
mkdir app
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
  /tmp/migration-1.0-SNAPSHOT.jar 2>&1 | tee debug.log
```

# Optimize 

## Filters

If you have large data in prod, try to use filters to do the migration, for example we can try to use date to split the data
```
Dataset<Row> sqlDF = spark.sql("SELECT * FROM <tablename> where <date_column> > '2023-08-08'");
```
Or the filters when loading the data
```
.filter("some_column > some_value")
.select("relevant_column1", "relevant_column2");
```

## Resources
Another point, we can increase the resources (cpu & mem) to speed up the process
- worker resource
```
- resources:
    limits:
      cpu: '4'
      memory: 8Gi
    requests:
      cpu: 300m
      memory: 400Mi
```
- executor resource
```
.config("spark.executor.memory", "6g")
.config("spark.executor.cores", "4")
```

_NOTE_: executor with cores `4` will run 4 tasks in parallel 

## File Format

This application supports two file formats, parquet and CSV, ref the [config](config/config.yaml)

```
fileFormat: parquet
```

Parquet is designed to be highly efficient for both reading and writing large datasets.
[link](https://aemreusta.medium.com/parquet-vs-csv-a-comparison-of-file-formats-for-data-storage-with-experiment-bb0a4d7263ed)

# Running the migration - write/import

If there is no shared storage, you need to move the data into the target worker first.

```
oc rsync stage_pathmap_migration/ <spark-worker POD_NAME>:/opt/spark/storage/indy_pathmap_0729/
```

Update the migration app to write mode, and then sync the jar into spark worker, submit the job in worker node as above.

**_NOTE:_** If you migrate the data into a new cassandra cluster, please ref the above to deploy the spark master & worker in the new cluster as well. 

# Shared Storage - AWS S3

If you have shared storage, you will not need to transfer the csv files between Openshift cluster, you just need to configure the info of S3

```
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
BUCKET_REGION
BUCKET_NAME
```

You can use the following tool aws cli to check the files in S3, and remove them after migration.

Install the aws cli in the cluster.
```
oc apply -f s3/k8s/aws_cli.yaml
```
_*NOTE:*_ I mount the volume of spark worker here, then I can download the files first before importing.

```
volumes:
- name: vol-migration-data
  persistentVolumeClaim:
    claimName: vol-migration-data
```

The cmd to download the files from S3 to local.

```
aws s3 cp s3://<BUCKET_NAME>/indy_migration_test/indystorage/pathmap/ ./ --recursive
```

Login into the cli container, exec `aws s3` command with the following ENVs.

```
export AWS_ACCESS_KEY_ID=<>
export AWS_SECRET_ACCESS_KEY=<>
export AWS_DEFAULT_REGION=<>
```

```
aws s3 ls --human-readable --summarize s3://<BUCKET_NAME>/indy_migration_test/indystorage/pathmap/

aws s3 rm s3://<BUCKET_NAME>/indy_migration_test/ --recursive
```

# Write Tuning Parameters

During tests, I found that there is no big issue on reading from Cassandra. And just need to pay attention to the write operation.
Try to decrease the value for each if you have smaller resources for your Cassandra. 

```
.config("spark.cassandra.output.consistency.level", "ONE") // QUORUM
.config("spark.cassandra.output.batch.size.rows", "500") // 2000 for pathmap
.config("spark.cassandra.output.batch.size.bytes", "1048576") // 10485760 (10M) for pathmap
.config("spark.cassandra.output.concurrent.writes", "5") // 50 for pathmap
```

For example, the following is the config for folo.records.
```
- keyspace: folo
  table: records2
  tempView: records2
  id: records2
  # Write Tuning Parameters
  outputConsistencyLevel: ONE
  outputBatchSizeRows: 500
  outputBatchSizeBytes: 524288
  outputConcurrentWrites: 5
```