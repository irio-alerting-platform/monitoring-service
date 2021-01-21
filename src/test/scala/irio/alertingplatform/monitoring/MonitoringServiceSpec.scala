package irio.alertingplatform.monitoring

import akka.actor.ActorSystem
import irio.alertingplatform.monitoring.MonitoringRunnable.MonitoringUrl
import irio.alertingplatform.monitoring.MonitoringServiceDto.{
  MonitoringUrlDto,
  MonitoringUrlsRequest,
  MonitoringUrlsResponse
}
import irio.alertingplatform.utils.LoggingSupport
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import redis.clients.jedis.Jedis

class MonitoringServiceSpec(implicit ee: ExecutionEnv)
    extends SpecificationLike
    with MockitoSugar
    with ArgumentMatchersSugar
    with LoggingSupport {

  implicit val system = ActorSystem("test")

  trait TestCase extends Scope {
    val monitoringSchedulerService = mock[MonitoringSchedulerService]
    val redisClient                = mock[Jedis]
    val monitoringService          = new MonitoringService(monitoringSchedulerService, redisClient)
  }

  def generateDto(url: MonitoringUrl): MonitoringUrlDto = MonitoringUrlDto(
    url                       = url.url,
    adminFst                  = url.adminFst.getAddress,
    adminSnd                  = url.adminSnd.getAddress,
    frequencyMillis           = url.frequency.toMillis,
    alertingWindow            = url.alertingWindow,
    allowedResponseTimeMillis = url.allowedResponseTime.toMillis
  )

  def generateRequest(urls: List[MonitoringUrlDto], externalIp: String): MonitoringUrlsRequest =
    MonitoringUrlsRequest(urls, externalIp)

  "getNewUrls" should {
    "return URLs received for monitoring" in new TestCase {
      // given
      val externalIp            = "0.0.0.0"
      val urls                  = List.range(0, 4).map(id => MonitoringUrlGenerator.generate(id = id, externalIp = externalIp))
      val monitoringUrlsRequest = generateRequest(urls.map(generateDto), externalIp)

      // when
      val result = monitoringService.getUrlsToMonitor(monitoringUrlsRequest)

      // then
      result must beEqualTo(MonitoringUrlsResponse(monitoringUrlsRequest.urls.map(_.url))).await
    }
    "return empty list if URLs list is empty" in new TestCase {
      // given
      val externalIp            = "0.0.0.0"
      val urls                  = List()
      val monitoringUrlsRequest = generateRequest(urls.map(generateDto), externalIp)

      // when
      val result = monitoringService.getUrlsToMonitor(monitoringUrlsRequest)

      // then
      result must beEqualTo(MonitoringUrlsResponse(monitoringUrlsRequest.urls.map(_.url))).await
    }
  }

}
