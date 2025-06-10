#!/bin/bash
set -e

echo "Waiting for ZooKeeper to be ready..."
/wait-for-it.sh zookeeper:2181 --timeout=60 --strict -- echo "ZooKeeper is up"

echo "Removing stale broker registration node /brokers/ids/1 if it exists..."
echo "delete /brokers/ids/1" | zkCli.sh -server zookeeper:2181 || echo "Node /brokers/ids/1 not found, continuing..."

echo "Starting Kafka broker..."
exec /etc/confluent/docker/run