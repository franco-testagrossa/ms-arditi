# akka cluster on kubernetes cluster

# How to run
### Local development
Run `sbt` on the root folder. Once you are in there:
- execute run

Run kafka broker
docker run -d -p 2181:2181 -p 9092:9092 -e ADVERTISED_HOST=127.0.0.1  -e NUM_PARTITIONS=1 johnnypark/kafka-zookeeper

Run cassandra db
docker run cassandra

### Local cluster development
./minikube.sh
kubectl get all
kubectl logs <pod_id>

-------------------------------------
## Basics
### sbt inside the root-project
- execute `clean` to apply the command to all *sub projects*
- execute `compile` to apply the command to all *sub projects*
- execute `test` to run all the tests in all the *sub projects*
- execute `testc` to run the test coverage report in all the *sub projects*

### sbt inside a sub-project
- execute `clean` to clean the target directory in the current *project*
- execute `compile` to compile in the current *project*
- execute `test` to run all the tests in the current *project*
- execute `testOnly *<spec_name>` to run a single spec in the current *project*
- execute `testc` to run the test coverage report process in the current *project*

### how to disable coverage in source code
```scala
// $COVERAGE-OFF$Reason for disabling coverage
// ...
// $COVERAGE-ON$
```

------------------------------------
# Kafka interaction
In this script we show you how to run a producer of data to populate Kafka
with JSON-serialized messages, which are then consumed by the app
which then exposes via Akka HTTP the state of the aggregators.


# How to run
docker run -d -p 2181:2181 -p 9092:9092 -e ADVERTISED_HOST=127.0.0.1  -e NUM_PARTITIONS=1 johnnypark/kafka-zookeeper
// sudo kill `sudo lsof -t -i:2551`
sbt 'runMain transaction.ProducerApp'
kafkacat -L -b 0.0.0.0:9092
kafkacat -C -b 0.0.0.0:9092 -t stage1
sbt 'runMain Main' &

sleep 60

curl http://0.0.0.0:8080/state/Person/1.0


# Docker-compose
This script is similar to kafka interaction.
However, in this script we show you how to use docker-compose to create a production-like enviroment


# How to run
docker-compose kill
docker-compose up -d zookeeper
sleep 10
docker-compose up -d kafka
sleep 10
sbt 'runMain transaction.ProducerApp'
kafkacat -L -b 0.0.0.0:9094
kafkacat -C -b 0.0.0.0:9094 -t SOURCE_TOPIC
docker-compose up -d seed
sleep 15
curl http://0.0.0.0:8080/state/Person/1.0
