FROM openjdk:11-jdk

RUN apt-get update && apt-get install -f && apt-get install -y curl && apt-get install -y scala

ENV SPARK_VERSION=2.4.8
ENV HADOOP_VERSION=2.8
ENV SPARK_PACKAGE spark-${SPARK_VERSION}-bin-without-hadoop
ENV SPARK_HOME=/opt/spark

RUN mkdir -p "$SPARK_HOME" \
    && curl -sL --retry 3 "https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-$SPARK_VERSION-bin-hadoop$HADOOP_VERSION.tgz" \
    | tar -xzC "$SPARK_HOME" --strip-components=1

RUN curl -L -o /opt/jars/spark-core.jar https://repo1.maven.org/maven2/org/apache/spark/spark-core_2.11/2.4.8/spark-core_2.11-2.4.8.jar \
    && curl -L -o /opt/jars/spark-sql.jar https://repo1.maven.org/maven2/org/apache/spark/spark-sql_2.11/2.4.8/spark-sql_2.11-2.4.8.jar \
    && curl -L -o /opt/jars/play-json.jar https://repo1.maven.org/maven2/com/typesafe/play/play-json_2.11/2.6.0/play-json_2.11-2.6.0.jar \
    && curl -L -o /opt/jars/json4s-native.jar https://repo1.maven.org/maven2/org/json4s/json4s-native_2.11/3.6.11/json4s-native_2.11-3.6.11.jar


ENV PATH="$SPARK_HOME/bin:$PATH"

RUN wget -P /opt/jars/ https://repo1.maven.org/maven2/org/json4s/json4s-native_2.12/3.6.11/json4s-native_2.12-3.6.11.jar

COPY target/scala-2.13/data-json_2.13-0.1.0-SNAPSHOT.jar /opt/app/data-json_2.13-0.1.0-SNAPSHOT.jar

WORKDIR /opt/app

CMD ["spark-submit", "--master",  "local[*]", "--jars", "/opt/jars", "--class", "MainSparkSubmit", "data-json_2.13-0.1.0-SNAPSHOT.jar"]