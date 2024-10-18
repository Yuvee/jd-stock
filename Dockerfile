FROM bellsoft/liberica-openjdk-debian:17.0.11-cds

LABEL maintainer="zhuweitung"

RUN mkdir -p /app/jd-stock/log \
    /app/jd-stock/config \
    /app/jd-stock/data

WORKDIR /app/jd-stock

ENV LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

ADD ./target/jd-stock-jar-with-dependencies.jar ./app.jar
ADD ./data/area_code.json ./data/area_code.json

VOLUME ./config

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar

