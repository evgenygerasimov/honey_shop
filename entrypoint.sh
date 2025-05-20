#!/bin/bash

./wait-for-it.sh zookeeper:2181 --timeout=60 --strict -- echo "Zookeeper is up"
./wait-for-it.sh kafka:9092 --timeout=60 --strict -- echo "Kafka is up"
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

exec java -jar app.jar