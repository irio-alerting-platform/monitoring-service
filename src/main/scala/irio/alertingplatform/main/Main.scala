package irio.alertingplatform.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.config.{Config, ConfigFactory}
import irio.alertingplatform.mailer.MailerService
import irio.alertingplatform.mailer.MailerServiceConfig.MailerConfig
import irio.alertingplatform.monitoring.{MonitoringSchedulerService, MonitoringService, MonitoringServiceRouter}
import irio.alertingplatform.redis.RedisConfig.RedisConfig
import irio.alertingplatform.utils.LoggingSupport
import javax.mail.internet.InternetAddress
import redis.clients.jedis.Jedis

object Main extends App with LoggingSupport {
  implicit val system = ActorSystem("monitoring-system")
  implicit val ec     = system.dispatcher

  private val config: Config = ConfigFactory.load()

  private val httpPort = config.getInt("http.port")

  private lazy val mailerConfig = MailerConfig(
    config.getString("mailer.host"),
    config.getInt("mailer.port"),
    new InternetAddress(config.getString("mailer.from")),
    config.getString("mailer.pass"),
    (externalIp, id) => s"http://$externalIp:$httpPort/monitoring/mailer/$id"
  )

  private lazy val redisConfig = RedisConfig(config.getString("redis.host"), config.getInt("redis.port"))
  private lazy val redisClient = new Jedis(redisConfig.host, redisConfig.port)

  private lazy val monitoringSchedulerService = new MonitoringSchedulerService(mailerService)
  private lazy val mailerService              = new MailerService(mailerConfig, redisClient)
  private lazy val monitoringService          = new MonitoringService(monitoringSchedulerService, redisClient)
  private lazy val router                     = new MonitoringServiceRouter(monitoringService)

  private val route = router.routes

  Http()
    .newServerAt(config.getString("http.interface"), config.getInt("http.port"))
    .bind(route)
}
