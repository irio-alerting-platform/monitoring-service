package irio.alertingplatform.redis

import com.redis.RedisClient
import irio.alertingplatform.redis.RedisConfig.RedisConfig

class MonitoringRedisClient(config: RedisConfig) {

  val redis = new RedisClient(config.host, config.port)

}
