package poc

import com.typesafe.config.Config

class AppConfig(config: Config) {
  // API
  lazy val API_HOST: String = config.getString("application.api.host") // "0.0.0.0"
  lazy val API_PORT: Int = config.getInt("application.api.port") // 5000

  // KAFKA
  lazy val KAFKA_BROKER: String = config.getString("kafka.brokers")
  lazy val CONSUMER_GROUP: String = "CONSUMER_GROUP"
  lazy val SOURCE_TOPIC: String = "SOURCE_TOPIC"
  lazy val SINK_TOPIC: String = "SINK_TOPIC"
}
