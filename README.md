# Cassandra Migration 

This is an option that we can use to do cassandra migration, using the lib [spark-cassandra-connector](https://github.com/datastax/spark-cassandra-connector). 
It's helpful when we want to migrate cassandra to a new cluster, at least it's an option.

# Prerequisites

To complete this, you need:
- Spark 3.4.0
- Cassandra 3.11.X
- Openshift 4
- JDK 11+

Build spark master and worker, for details of spark, ref the [doc](spark/README.md)
```
docker build -t quay.io/sswguo/spark-master:3.4.0-jdk11-1.2 .
docker push quay.io/sswguo/spark-master:3.4.0-jdk11-1.2

docker build -t quay.io/sswguo/spark-worker:3.4.0-jdk11-1.2 .
docker push quay.io/sswguo/spark-worker:3.4.0-jdk11-1.2
```

Deploy spark
```
oc apply -f spark-master-deployment.yaml
oc apply -f spark-master-service.yaml
oc apply -f spark-worker-persistent-volume-claim.yaml
oc apply -f spark-worker-deployment.yaml 
```

Build the migration application
```
mvn clean package
```

Sync the jar into spark worker
```
cp target/migration-1.0-SNAPSHOT.jar app
oc rsync app/ <spark-worker POD_NAME>:/tmp/
```

Submit the job in worker node
```
spark-submit \
  --class org.commonjava.migration.App \
  --master spark://spark-master:7077 \
  /tmp/migration-1.0-SNAPSHOT.jar
```