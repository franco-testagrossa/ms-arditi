package common.kafka.utils

object Startup extends App {

  start()

  def start() = {
    import sys.process._

    val startupCommand = """
      | docker run
      | -d
      | -p 2181:2181
      | -p 9092:9092
      | -e ADVERTISED_HOST=127.0.0.1
      | -e NUM_PARTITIONS=1
      | johnnypark/kafka-zookeeper
      |
      | """
      .stripMargin
      .replaceAll("\n", " ")
      .trim

    def y(s: String) = Console.YELLOW + s + Console.RESET
    def r(s: String) = Console.RED + s + Console.RESET

    val logger = ProcessLogger(
      (dockerContainerId: String) =>
        println(
          y(s"""
               | Started Kafka inside a Docker container in the background!
               | We'll give it a minute to start and then we can assume it done! :D
               | ${dockerContainerId}
               | """)),
      (e: String) =>
        if (e contains "Bind for 0.0.0.0:9092 failed: port is already allocated.")
          println(y("The container is up and running already, so no problem! :)"))
        else
          println(r(e)))

    println(startupCommand)
    startupCommand ! logger

  }

}
