# Spark Deployment in Openshift

**NOTE:**  

Here I am still using the same `entrypoint.sh` for both master and worker, which means I need to comment out start-worker.sh
when building master and comment out spark-master.sh when building worker image.

Master, specifying the master port in environment
```
env:
- name: SPARK_MASTER_PORT
  value: '7077'
```

Worker, specifying the master url in environment
```
env:
- name: SPARK_MASTER_URL
  value: "spark://spark-master:7077"
```
_NOTE_: For PVC, update the `storageClassName` according to your cluster

And please specify your resources based on your business, otherwise it will be killed due to lack of enough resources
```
resources:
  limits:
    cpu: 2
    memory: 2000Mi
  requests:
    cpu: 300m
    memory: 400Mi
```

Logs
```
> oc rsh <POD_NAME>
> tail -200f /opt/spark/logs/spark--org.apache.spark.deploy.master.Master-1-<POD_NAME>.out
```

Web UI
```
> oc expose svc/spark-master 
```
_NOTE_: Check if the `targetPort` pointing to `webui(8080)`, the default port is `7077`


If you want to custom more variables, check the following template in `/opt/spark/conf/`, and update the [Dockerfile](build/Dockerfile) to include them.
```
-rw-r--r--. 1 spark spark 1.1K Apr  7  2023 fairscheduler.xml.template
-rw-r--r--. 1 spark spark 3.3K Apr  7  2023 log4j2.properties.template
-rw-r--r--. 1 spark spark 9.0K Apr  7  2023 metrics.properties.template
-rw-r--r--. 1 spark spark 1.3K Apr  7  2023 spark-defaults.conf.template
-rwxr-xr-x. 1 spark spark 4.6K Apr  7  2023 spark-env.sh.template
-rw-r--r--. 1 spark spark  865 Apr  7  2023 workers.template
```