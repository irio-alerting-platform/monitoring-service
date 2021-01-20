package irio.alertingplatform.monitoring

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import irio.alertingplatform.mailer.MailerService
import irio.alertingplatform.monitoring.MonitoringService.{ErrorCount, MonitoringUrl}
import irio.alertingplatform.monitoring.MonitoringServiceConfig.MonitoringConfig
import irio.alertingplatform.monitoring.MonitoringServiceDto.{
  MonitoringUrlDto,
  MonitoringUrlsRequest,
  MonitoringUrlsResponse
}
import irio.alertingplatform.redis.MonitoringRedisClient
import irio.alertingplatform.utils.LoggingSupport
import javax.mail.internet.InternetAddress

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class MonitoringService(config: MonitoringConfig, mailerService: MailerService, redisClient: MonitoringRedisClient)(
  implicit
  ec: ExecutionContext,
  system: ActorSystem,
  mat: Materializer
) extends LoggingSupport {

  private val InitialDelayMillis     = FiniteDuration(config.initialDelayMillis, TimeUnit.MILLISECONDS)
  private val FirstEmailInitialDelay = FiniteDuration(0, TimeUnit.MILLISECONDS)
  private val BlockingDispatcher     = "monitoring-blocking-dispatcher"
  private val EmailDispatcher        = "email-blocking-dispatcher"

  private val runningTasks     = mutable.ArrayBuffer[Cancellable]().empty
  private val monitoringErrors = mutable.ArrayBuffer[ErrorCount]().empty

  /**
    * Received URLs from scheduler and distributes them between workers to monitor
    * @param request Request with URLs to be monitored
    * @return response with received URLs
    */
  def getUrlsToMonitor(request: MonitoringUrlsRequest): Future[MonitoringUrlsResponse] = {
    implicit val ec = system.dispatchers.lookup(BlockingDispatcher)
    val urls        = request.urls
    val externalIp  = request.externalIp
    logger.info("Received URLs to monitor {}, external ip {}", urls, externalIp)

    /* Cancel all previous monitoring workers */
    runningTasks.foreach(cancellable => cancellable.cancel())

    /* Give each URL an id and start workers for new URLs */
    urls.zipWithIndex.foreach {
      case (url, id) =>
        logger.info("{}: {}", id, url.url)
        val DelayMillis = FiniteDuration(url.frequencyMillis, TimeUnit.MILLISECONDS)
        runningTasks.addOne {
          system.scheduler.scheduleWithFixedDelay(InitialDelayMillis, DelayMillis)(
            monitoringRunnable(MonitoringUrl.apply(id, url, externalIp))
          )
        }
        monitoringErrors.addOne(new ErrorCount(url.alertingWindow))
    }

    Future.successful(MonitoringUrlsResponse(request.urls.map(_.url)))
  }

  /**
    * Handles admin clicking confirmation link.
    * @param id of the URL
    * @return id of the URL
    */
  def handleAdminResponse(id: Int): Future[Int] = {
    redisClient.redis.del(id).foreach(res => logger.info("Deleted {} keys for id {}", res, id))
    Future.successful(id)
  }

  /**
    * Sends a request to monitored URL and collects the results.
    * Results are collected in [[monitoringErrors]] list.
    * Updating [[monitoringErrors]] is thread-safe using `synchronized` keyword primitive.
    * @param monitoringUrl contains monitored URL with id.
    * @return a new [[Runnable]] worker.
    */
  private def monitoringRunnable(monitoringUrl: MonitoringUrl): Runnable =
    () => {
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
          if (updateCount(monitoringUrl)) {
            logger.warn("Error limit reached for URL {}", monitoringUrl)
            alertUsers(monitoringUrl)
          }
        case _ => logger.warn("Request didn't return HttpResponse")
      }
    }

  /**
    * Sends a request to monitored service to health check it.
    * @param url to be monitored.
    * @return response from monitored service.
    */
  private def sendMonitoringRequest(url: String): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(method = HttpMethods.GET, uri = Uri(url)))

  /**
    * Updates error count for URL specified in the argument.
    * @param monitoringUrl contains monitored URL with id.
    * @return true if error limit was reached, false otherwise.
    */
  private def updateCount(monitoringUrl: MonitoringUrl): Boolean = {
    val errorCount = monitoringErrors(monitoringUrl.id)
    errorCount.synchronized {
      errorCount.count -= 1
      if (errorCount.count == 0) {
        errorCount.count = monitoringUrl.alertingWindow
        return true
      } else
        return false
    }
  }

  /**
    * Alert users about errors in monitored service.
    * @return
    */
  private def alertUsers(monitoringUrl: MonitoringUrl) = {
    implicit val ec = system.dispatchers.lookup(EmailDispatcher)
    system.scheduler.scheduleOnce(FirstEmailInitialDelay)(mailerService.sendMail(monitoringUrl))
    system.scheduler.scheduleOnce(monitoringUrl.allowedResponseTimeMillis)(mailerService.sendBackupMail(monitoringUrl))
  }
}

object MonitoringService {
  case class MonitoringUrl(
    id: Int,
    url: String,
    adminFst: InternetAddress,
    adminSnd: InternetAddress,
    alertingWindow: Int,
    allowedResponseTimeMillis: FiniteDuration,
    externalIp: String
  )
  object MonitoringUrl {
    def apply(id: Int, dto: MonitoringUrlDto, externalIp: String): MonitoringUrl =
      MonitoringUrl(
        id,
        dto.url,
        new InternetAddress(dto.adminFst),
        new InternetAddress(dto.adminSnd),
        dto.alertingWindow,
        FiniteDuration(dto.allowedResponseTimeMillis, TimeUnit.MILLISECONDS),
        externalIp
      )
  }

  class ErrorCount(var count: Int)
}
