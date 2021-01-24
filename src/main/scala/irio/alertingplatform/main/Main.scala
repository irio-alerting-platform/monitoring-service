package irio.alertingplatform.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.{Config, ConfigFactory}
import irio.alertingplatform.healthcheck.HealthcheckRouter
import irio.alertingplatform.mailer.MailerService
import irio.alertingplatform.mailer.MailerServiceConfig.MailerConfig
import irio.alertingplatform.monitoring.{MonitoringSchedulerService, MonitoringService, MonitoringServiceRouter}
import irio.alertingplatform.redis.RedisConfig.RedisConfig
import irio.alertingplatform.utils.LoggingSupport
import redis.clients.jedis.Jedis

import scala.concurrent.ExecutionContext

object Main extends App with LoggingSupport {
  implicit val system: ActorSystem  = ActorSystem("monitoring-system")
  implicit val ec: ExecutionContext = system.dispatcher

  private val config: Config = ConfigFactory.load()

  private lazy val mailerConfig = MailerConfig(config)

  private lazy val redisConfig = RedisConfig(config.getString("redis.host"), config.getInt("redis.port"))
  private lazy val redisClient = new Jedis(redisConfig.host, redisConfig.port)

  private lazy val healthcheckRouter = new HealthcheckRouter()

  private lazy val monitoringSchedulerService = new MonitoringSchedulerService(mailerService, redisClient)
  private lazy val mailerService              = new MailerService(mailerConfig, redisClient)
  private lazy val monitoringService          = new MonitoringService(monitoringSchedulerService, redisClient)
  private lazy val monitoringRouter           = new MonitoringServiceRouter(monitoringService)

  private val route = healthcheckRouter.routes ~ monitoringRouter.routes

  Http()
    .newServerAt(config.getString("http.interface"), config.getInt("http.port"))
    .bind(route)
}
