package poc

import com.typesafe.config.Config

class AppConfig(config: Config) {
  // API
  val API_HOST: String = config.getString("application.api.host") // "0.0.0.0"
  val API_PORT: Int = config.getInt("application.api.port") // 5000

  // KAFKA
  val KAFKA_BROKER: String = config.getString("kafka.brokers")
  val DATA_GROUP: String = "DATA_GROUP"
  val SOURCE_TOPIC: String = "sourceTopic"
  val SINK_TOPIC: String = "sinkTopic"
}
