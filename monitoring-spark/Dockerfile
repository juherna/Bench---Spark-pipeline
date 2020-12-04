# Container image for Spark Dashboard
# using InfluxDB

FROM ubuntu:20.04

ENV INFLUXDB_VERSION 1.8.3
ENV ARCH amd64

RUN set -ex && \
    apt-get update && \
    apt-get install -qq -y curl libfontconfig && \
    curl -O https://dl.influxdata.com/influxdb/releases/influxdb_${INFLUXDB_VERSION}_${ARCH}.deb && \
    dpkg -i influxdb_${INFLUXDB_VERSION}_${ARCH}.deb && \
    rm -f influxdb_${INFLUXDB_VERSION}_${ARCH}.deb

COPY influxdb.conf /etc/influxdb/influxdb.conf
COPY entrypoint.sh /opt/entrypoint.sh

# expose influxdb graphite (2003) and http (8086) endpoints
EXPOSE 2003/tcp 8086/tcp

WORKDIR /
ENTRYPOINT [ "/opt/entrypoint.sh" ]
