#!/usr/bin/env bash
INITIAL_LOAD=5
while true; do
    read -p "First time doing jokes? [y/n]" yn
    case $yn in
        [Yy]* ) INITIAL_LOAD=60; break;;
        [Nn]* ) break;;
        * ) echo "Please answer yes or no.";;
    esac
done

docker run -d -p 2181:2181 -p 9092:9092 -e ADVERTISED_HOST=127.0.0.1  -e NUM_PARTITIONS=1 johnnypark/kafka-zookeeper

sleep $INITIAL_LOAD

Waiter(){
  sleep 10
  echo "cook me an egg!" | kafkacat -P -b 0.0.0.0:9092 -t test
}

DumbWaiter(){
  sleep 15
  echo "cook me a hardboiled egg!" | kafkacat -P -b 0.0.0.0:9092 -t test
}

DumbWaiter &
Waiter &

sbt 'runMain ConsumerApp'



// kafkacat -b 0.0.0.0:9092 -t test

