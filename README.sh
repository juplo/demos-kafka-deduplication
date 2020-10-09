#!/bin/bash

if [ "$1" = "cleanup" ]
then
  docker-compose down
  mvn clean
  exit
fi

if [[ $(docker image ls -q juplo/deduplicator:streams) == "" ]]
then
  mvn package
  docker build -t juplo/deduplicator:streams .
else
  echo "Using image existing image:"
  docker image ls juplo/deduplicator:streams
fi

docker-compose up -d zookeeper kafka

while ! [[ $(zookeeper-shell zookeeper:2181 ls /brokers/ids 2> /dev/null) =~ 1001 ]];
do
  echo "Waiting for kafka...";
  sleep 1;
done

kafka-topics --create --zookeeper zookeeper:2181 --replication-factor 1 --partitions 3 --topic input
kafka-topics --create --zookeeper zookeeper:2181 --replication-factor 1 --partitions 3 --topic output

docker-compose up -d deduplicator

while ! [[ $(http :8080/actuator/health 2>/dev/null | jq -r .components.streams.status) == "RUNNING" ]];
do
  echo "Waiting for Streams-Application...";
  sleep 1;
done

cat data.txt | kafkacat -K: -b localhost:9092 -t input

kafkacat -C -b localhost:9092 -t input -e | wc -l
kafkacat -C -b localhost:9092 -t output -e | wc -l

kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | grep ^0 > result_0.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | grep ^1 > result_1.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | grep ^2 > result_2.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | grep ^3 > result_3.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | grep ^4 > result_4.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | grep ^5 > result_5.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | grep ^6 > result_6.txt
