FROM registry.access.redhat.com/ubi7:7.9

#FROM registry.access.redhat.com/ubi8/openjdk-11:1.11

USER root

ARG spark_uid=185

RUN groupadd --system --gid=${spark_uid} spark && \
    useradd --system --uid=${spark_uid} --gid=spark spark

ENV TZ UTC

RUN yum -y update && \
    yum -y install wget git tar which curl tree java-11-openjdk-devel net-tools lsof iotop hostname && \
    yum clean all && \
    rm -rf /var/cache/yum

# Install Spark
ARG SPARK_VERSION=3.4.0
ARG HADOOP_VERSION=3
ENV SPARK_HOME=/opt/spark
RUN wget -qO- https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz | tar xvz -C /opt/ && \
    mv /opt/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION} ${SPARK_HOME} && \
    ln -s ${SPARK_HOME}/bin/* /usr/bin/

# Set the working directory
WORKDIR ${SPARK_HOME}

# Copy entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

RUN chown -R spark:spark ${SPARK_HOME}

COPY spark-defaults.conf ${SPARK_HOME}/conf/
COPY log4j2.properties ${SPARK_HOME}/conf/

RUN mkdir -p ${SPARK_HOME}/logs ${SPARK_HOME}/work ${SPARK_HOME}/storage \
         && chown -R spark:spark ${SPARK_HOME}/logs ${SPARK_HOME}/work ${SPARK_HOME}/storage ${SPARK_HOME}/conf \
         && chmod 777 ${SPARK_HOME}/logs ${SPARK_HOME}/work ${SPARK_HOME}/storage ${SPARK_HOME}/conf

USER spark

ENTRYPOINT ["/entrypoint.sh"]