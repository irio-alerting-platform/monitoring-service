package irio.alertingplatform.monitoring

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Cancellable}
import akka.stream.Materializer
import irio.alertingplatform.mailer.MailerService
import irio.alertingplatform.monitoring.MonitoringRunnable.MonitoringUrl
import irio.alertingplatform.monitoring.MonitoringServiceConfig.MonitoringConfig
import irio.alertingplatform.monitoring.MonitoringServiceDto.{MonitoringUrlsRequest, MonitoringUrlsResponse}
import irio.alertingplatform.redis.MonitoringRedisClient
import irio.alertingplatform.utils.LoggingSupport

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class MonitoringService(config: MonitoringConfig, mailerService: MailerService, redisClient: MonitoringRedisClient)(
  implicit
  system: ActorSystem,
  mat: Materializer
) extends LoggingSupport {

  private val runningTasks = mutable.ArrayBuffer[Cancellable]().empty

  implicit val blockingDispatcher: ExecutionContext = system.dispatchers.lookup("monitoring-blocking-dispatcher")

  /**
    * Received URLs from scheduler and distributes them between workers to monitor.
    *
    * @param request Request with URLs to be monitored
    * @return response with received URLs
    */
  def getUrlsToMonitor(request: MonitoringUrlsRequest): Future[MonitoringUrlsResponse] = {
    val urls       = request.urls
    val externalIp = request.externalIp
    logger.info("Received URLs {}, external IP {}", urls, externalIp)

    /* Cancel all previous monitoring workers */
    runningTasks.foreach(cancellable => cancellable.cancel())

    /* Give each URL an id and start workers for new URLs */
    urls.zipWithIndex.foreach {
      case (url, id) =>
        val monitoringUrl = MonitoringUrl.apply(id, url, externalIp)
        runningTasks.addOne {
          system.scheduler.scheduleWithFixedDelay(monitoringUrl.initialDelay, monitoringUrl.frequency) {
            new MonitoringRunnable(monitoringUrl, mailerService)
          }(blockingDispatcher)
        }
    }

    Future.successful(MonitoringUrlsResponse(request.urls.map(_.url)))
  }

  /**
    * Handles admin clicking confirmation link.
    *
    * @param id of the URL
    * @return id of the URL
    */
  def handleAdminResponse(id: Int): Future[Int] = {
    redisClient.redis.del(id).foreach(res => logger.info("Deleted {} keys for id {}", res, id))
    Future.successful(id)
  }

}
