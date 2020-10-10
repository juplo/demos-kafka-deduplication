#!/bin/bash

if [ "$1" = "cleanup" ]
then
  docker-compose down
  mvn clean
  rm *.txt
  exit
fi

if [[ $(docker image ls -q juplo/deduplicator:streams) == "" || "$1" = "build" ]]
then
  mvn package || exit
  docker build -t juplo/deduplicator:streams . || exit
else
  echo "Using image existing image:"
  docker image ls juplo/deduplicator:streams
fi

docker-compose up -d zookeeper kafka

if [ ! -e data.txt ];
then
  echo ./create-data.sh
fi

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

kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | awk -F: '/^peter/ { print $2 }' > result_peter.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | awk -F: '/^franz/ { print $2 }' > result_franz.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | awk -F: '/^ute/   { print $2 }' > result_ute.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | awk -F: '/^klaus/ { print $2 }' > result_klaus.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | awk -F: '/^paul/  { print $2 }' > result_paul.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | awk -F: '/^petra/ { print $2 }' > result_petra.txt
kafkacat -C -b localhost:9092 -t output -e  -f'%k:%s\n' | awk -F: '/^siggi/ { print $2 }' > result_siggi.txt
