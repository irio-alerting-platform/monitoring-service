package irio.alertingplatform.monitoring

import akka.actor.{ActorSystem, Cancellable}
import akka.stream.Materializer
import irio.alertingplatform.monitoring.MonitoringRunnable.MonitoringUrl
import irio.alertingplatform.monitoring.MonitoringServiceDto.{MonitoringUrlsRequest, MonitoringUrlsResponse}
import irio.alertingplatform.utils.LoggingSupport
import redis.clients.jedis.Jedis

import scala.collection.mutable
import scala.concurrent.Future

class MonitoringService(monitoringSchedulerService: MonitoringSchedulerService, redisClient: Jedis)(
  implicit
  system: ActorSystem,
  mat: Materializer
) extends LoggingSupport {

  private val runningTasks = mutable.ArrayBuffer[Cancellable]().empty

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
        runningTasks.addOne(
          monitoringSchedulerService.scheduleMonitoringRunnable(MonitoringUrl.apply(id, url, externalIp))
        )
    }

    Future.successful(MonitoringUrlsResponse(request.urls.map(_.url)))
  }

  /**
    * Handles admin clicking confirmation link.
    *
    * @param id of the URL
    * @return id of the URL
    */
  def handleConfirmation(id: Int): Future[Int] = {
    val res = redisClient.del(id.toString)
    logger.info("Deleted {} keys for id {}", res, id)
    Future.successful(id)
  }

}
