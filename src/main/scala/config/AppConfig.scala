package config

import com.typesafe.config.Config

class AppConfig(config: Config) {
  // API
  lazy val API_HOST: String = config.getString("http.ip")
  lazy val API_PORT: Int = config.getInt("http.port")

  lazy val NODEID: String = config.getString("clustering.ip")

  // KAFKA
  lazy val KAFKA_BROKER: String = config.getString("kafka.brokers")
  lazy val CONSUMER_GROUP: String = "CONSUMER_GROUP"
  lazy val SOURCE_TOPIC: String = "SOURCE_TOPIC"
  lazy val SINK_TOPIC: String = "SINK_TOPIC"
}
