package irio.alertingplatform.mailer

import java.util.UUID

import akka.actor.ActorSystem
import courier.{Envelope, Mailer, Text}
import irio.alertingplatform.mailer.MailerServiceConfig.MailerConfig
import irio.alertingplatform.monitoring.MonitoringRunnable.MonitoringUrl
import irio.alertingplatform.utils.LoggingSupport
import javax.mail.internet.InternetAddress
import redis.clients.jedis.Jedis

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class MailerService(config: MailerConfig, redisClient: Jedis)(
  implicit
  system: ActorSystem,
  ec: ExecutionContext
) extends LoggingSupport {

  private[mailer] def mailer: Mailer =
    Mailer(config.host, config.port)
      .auth(true)
      .as(config.from.toString, config.pass)
      .startTls(true)()

  /**
    * Insert a new mapping to redis and sends e-mail to the first admin.
    *
    * @param monitoringUrl monitored URL
    */
  def sendMail(monitoringUrl: MonitoringUrl): Unit = {
    logger.info("Sending email from {} to first admin {}", config.from, monitoringUrl.adminFst)

    /* Insert URL to redis. Will be deleted if admin goes to confirmation link. */
    val res = redisClient.set(monitoringUrl.id.toString, "1")
    logger.info("Set {} key for id {} in redis", res, monitoringUrl.id.toString)

    send(config.from, monitoringUrl.adminFst, monitoringUrl.id, monitoringUrl.url, monitoringUrl.externalIp)
  }

  /**
    * If the mapping was not removed from redis, a backup e-mail is sent to the second admin.
    *
    * @param monitoringUrl monitored URL
    */
  def sendBackupMail(monitoringUrl: MonitoringUrl): Unit = {
    val res = redisClient.get(monitoringUrl.id.toString)
    logger.info("Got {} key for id {} from redis", res, monitoringUrl.id.toString)
    if (res != null) {
      logger.info("Sending backup email from {} to second admin {}", config.from, monitoringUrl.adminSnd)
      send(config.from, monitoringUrl.adminSnd, monitoringUrl.id, monitoringUrl.url, monitoringUrl.externalIp)
    }
  }

  private def send(
    fromAddr: InternetAddress,
    toAddr: InternetAddress,
    id: UUID,
    url: String,
    externalIp: String
  ): Unit = {
    val subject          = subjectString()
    val confirmationLink = confirmationLinkString(externalIp, id)
    val content          = contentString(url, confirmationLink)
    mailer(
      Envelope
        .from(fromAddr)
        .to(toAddr)
        .subject(subject)
        .content(Text(content))
    ).onComplete {
      case Failure(exception) => logger.error("Failed sending email to {} with exception {}", toAddr, exception)
      case Success(_)         => logger.info("Successfully sent email to {}", toAddr)
    }
  }

}
