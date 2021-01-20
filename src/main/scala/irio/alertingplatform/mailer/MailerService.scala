package irio.alertingplatform.mailer

import akka.actor.ActorSystem
import courier.{Envelope, Mailer, Text}
import irio.alertingplatform.mailer.MailerServiceConfig.MailerConfig
import irio.alertingplatform.monitoring.MonitoringRunnable.MonitoringUrl
import irio.alertingplatform.redis.MonitoringRedisClient
import irio.alertingplatform.utils.LoggingSupport
import javax.mail.internet.InternetAddress

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class MailerService(config: MailerConfig, redisClient: MonitoringRedisClient)(
  implicit
  system: ActorSystem,
  ec: ExecutionContext
) extends LoggingSupport {

  private val EmailSubject = "Alerting platform – services are down!"

  private val mailer = Mailer(config.host, config.port)
    .auth(true)
    .as(config.from.toString, config.pass)
    .startTls(true)()

  def sendMail(monitoringUrl: MonitoringUrl): Unit = {
    logger.info("Sending email from {} to {}", config.from, monitoringUrl.adminFst)

    /* Insert URL to redis. Will be deleted if admin goes to confirmation link. */
    redisClient.redis.set(monitoringUrl.id, false)

    send(config.from, monitoringUrl.adminFst, monitoringUrl.id, monitoringUrl.url, monitoringUrl.externalIp)
  }

  def sendBackupMail(monitoringUrl: MonitoringUrl): Unit =
    if (redisClient.redis.get(monitoringUrl.id).nonEmpty) {
      logger.info("Sending email from {} to {}", config.from, monitoringUrl.adminSnd)
      send(config.from, monitoringUrl.adminSnd, monitoringUrl.id, monitoringUrl.url, monitoringUrl.externalIp)
    }

  private def send(from: InternetAddress, to: InternetAddress, id: Int, url: String, externalIp: String) =
    mailer(
      Envelope
        .from(from)
        .to(to)
        .subject(EmailSubject)
        .content(Text(alertMessage(id, url, externalIp)))
    ).onComplete {
      case Failure(exception) => logger.error("Failed sending email to {} with exception {}", to, exception)
      case Success(_)         => logger.info("Successfully sent email to {}", to)
    }

  private def alertMessage(id: Int, url: String, externalIp: String) = {
    val responseLink = config.responseUrl(externalIp, id)
    s"Service $url is down. If you've read this e-mail, confirm by going to $responseLink."
  }
}
