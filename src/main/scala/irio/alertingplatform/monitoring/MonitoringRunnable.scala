package irio.alertingplatform.monitoring

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import irio.alertingplatform.mailer.MailerService
import irio.alertingplatform.monitoring.MonitoringRunnable.MonitoringUrl
import irio.alertingplatform.monitoring.MonitoringServiceDto.MonitoringUrlDto
import irio.alertingplatform.utils.LoggingSupport
import javax.mail.internet.InternetAddress
import redis.clients.jedis.Jedis

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Success}

class MonitoringRunnable(monitoringUrl: MonitoringUrl, mailerService: MailerService, redisClient: Jedis)(
  implicit system: ActorSystem,
  ec: ExecutionContext
) extends Runnable
    with LoggingSupport {

  private val FirstEmailInitialDelay = FiniteDuration(0, TimeUnit.MILLISECONDS)
  private val EmailDispatcher        = "email-blocking-dispatcher"

  implicit val emailDispatcher: ExecutionContext = system.dispatchers.lookup(EmailDispatcher)

  private var errorCount = 0

  /**
    * Sends a request to monitored URL and collects the results.
    * Results are collected in [[errorCount]].
    */
  override def run(): Unit = {
    logger.info("Monitoring URL {}", monitoringUrl)
    sendMonitoringRequest(monitoringUrl.url).onComplete {
      case Failure(exception) =>
        logger.error("Request failed for URL {} with exception {}", monitoringUrl, exception.getMessage)
      case Success(HttpResponse(statusCode @ StatusCodes.OK, _, requestEntity, _)) =>
        requestEntity.discardBytes()
        logger.info("Request returned status code {} for URL {}", statusCode, monitoringUrl)
      case Success(HttpResponse(statusCode, _, requestEntity, _)) =>
        requestEntity.discardBytes()
        logger.warn("Request returned status code {} for URL {}", statusCode, monitoringUrl)
        if (updateCount(monitoringUrl) && (redisClient.get(monitoringUrl.id.toString) == null)) {
          logger.warn("Error limit reached for URL {}", monitoringUrl)
          alertUsers(monitoringUrl)
        }
      case _ => logger.warn("Request didn't return HttpResponse")
    }(ec)
  }

  /**
    * Sends a request to monitored service.
    *
    * @param url to be monitored.
    * @return response from monitored service.
    */
  private def sendMonitoringRequest(url: String): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(method = HttpMethods.GET, uri = Uri(url)))

  /**
    * Updates [[errorCount]] for URL specified in the argument.
    *
    * @param monitoringUrl contains monitored URL with id.
    * @return true if error limit was reached, false otherwise.
    */
  private def updateCount(monitoringUrl: MonitoringUrl): Boolean = {
    errorCount += 1
    if (errorCount == monitoringUrl.alertingWindow) {
      errorCount = 0; true
    } else false
  }

  /**
    * Alert users about errors in monitored service.
    */
  private def alertUsers(monitoringUrl: MonitoringUrl) = {
    system.scheduler.scheduleOnce(FirstEmailInitialDelay)(mailerService.sendMail(monitoringUrl))(emailDispatcher)
    system.scheduler.scheduleOnce(monitoringUrl.allowedResponseTime)(mailerService.sendBackupMail(monitoringUrl))(
      emailDispatcher
    )
  }

}

object MonitoringRunnable {

  val InitialDelayUpperBoundMillis = 2000

  case class MonitoringUrl(
    id: UUID,
    url: String,
    externalIp: String,
    adminFst: InternetAddress,
    adminSnd: InternetAddress,
    alertingWindow: Int,
    initialDelay: FiniteDuration,
    frequency: FiniteDuration,
    allowedResponseTime: FiniteDuration
  )
  object MonitoringUrl {
    def apply(dto: MonitoringUrlDto, externalIp: String): MonitoringUrl =
      MonitoringUrl(
        id                  = UUID.randomUUID(),
        url                 = dto.url,
        externalIp          = externalIp,
        adminFst            = new InternetAddress(dto.adminFst),
        adminSnd            = new InternetAddress(dto.adminSnd),
        alertingWindow      = dto.alertingWindow,
        initialDelay        = FiniteDuration(Random.nextLong(InitialDelayUpperBoundMillis), TimeUnit.MILLISECONDS),
        frequency           = FiniteDuration(dto.frequencyMillis, TimeUnit.MILLISECONDS),
        allowedResponseTime = FiniteDuration(dto.allowedResponseTimeMillis, TimeUnit.MILLISECONDS)
      )
  }

  class ErrorCount(var count: Int)
}
