package irio.alertingplatform.monitoring

import akka.actor.{ActorSystem, Cancellable}
import irio.alertingplatform.mailer.MailerService
import irio.alertingplatform.monitoring.MonitoringRunnable.MonitoringUrl
import irio.alertingplatform.utils.LoggingSupport

import scala.concurrent.ExecutionContext

class MonitoringSchedulerService(mailerService: MailerService)(implicit system: ActorSystem) extends LoggingSupport {

  implicit val blockingDispatcher: ExecutionContext = system.dispatchers.lookup("monitoring-blocking-dispatcher")

  def scheduleMonitoringRunnable(monitoringUrl: MonitoringUrl): Cancellable =
    system.scheduler.scheduleWithFixedDelay(monitoringUrl.initialDelay, monitoringUrl.frequency) {
      new MonitoringRunnable(monitoringUrl, mailerService)
    }(blockingDispatcher)

}
