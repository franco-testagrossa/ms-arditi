sbt 'docker:stage'
sbt 'docker:publishLocal'

docker-compose up -d

sleep 60

sbt 'runMain poc.ProducerApp' &