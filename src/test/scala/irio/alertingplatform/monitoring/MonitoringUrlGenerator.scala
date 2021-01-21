package irio.alertingplatform.monitoring

import java.util.concurrent.TimeUnit

import irio.alertingplatform.monitoring.MonitoringRunnable.MonitoringUrl
import javax.mail.internet.InternetAddress

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

object MonitoringUrlGenerator {
  val RandomIntUpperBound  = 32
  val RandomLongUpperBound = 2000
  def generate(
    id: Int                             = Random.nextInt(2 ^ 10),
    url: String                         = "http://www.test.pl/",
    externalIp: String                  = "0.0.0.0",
    adminFst: InternetAddress           = new InternetAddress("a@b.pl"),
    adminSnd: InternetAddress           = new InternetAddress("a@c.pl"),
    alertingWindow: Int                 = Random.nextInt(RandomIntUpperBound),
    initialDelay: FiniteDuration        = FiniteDuration(Random.nextLong(RandomLongUpperBound), TimeUnit.MILLISECONDS),
    frequency: FiniteDuration           = FiniteDuration(Random.nextLong(RandomLongUpperBound), TimeUnit.MILLISECONDS),
    allowedResponseTime: FiniteDuration = FiniteDuration(Random.nextLong(RandomLongUpperBound), TimeUnit.MILLISECONDS)
  ): MonitoringUrl =
    MonitoringUrl(id, url, externalIp, adminFst, adminSnd, alertingWindow, initialDelay, frequency, allowedResponseTime)
}
