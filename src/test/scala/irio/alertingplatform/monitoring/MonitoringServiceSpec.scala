package irio.alertingplatform.monitoring

import akka.actor.ActorSystem
import irio.alertingplatform.monitoring.MonitoringRunnable.MonitoringUrl
import irio.alertingplatform.monitoring.MonitoringServiceDto.{
  MonitoringConfirmationResponse,
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
    "return received urls" in new TestCase {
      // given
      val externalIp            = "0.0.0.0"
      val urls                  = List.fill(5)(MonitoringUrlGenerator.generate(externalIp = externalIp))
      val monitoringUrlsRequest = generateRequest(urls.map(generateDto), externalIp)

      // when
      val result = monitoringService.getUrls(monitoringUrlsRequest)

      // then
      result must beEqualTo(MonitoringUrlsResponse(monitoringUrlsRequest.urls.map(_.url))).await
    }
    "return empty list if no urls were received" in new TestCase {
      // given
      val externalIp            = "0.0.0.0"
      val urls                  = List()
      val monitoringUrlsRequest = generateRequest(urls.map(generateDto), externalIp)

      // when
      val result = monitoringService.getUrls(monitoringUrlsRequest)

      // then
      result must beEqualTo(MonitoringUrlsResponse(monitoringUrlsRequest.urls.map(_.url))).await
    }
  }
  "handleConfirmation" should {
    "delete mapping from redis and return url id" in new TestCase {
      // given
      val url         = MonitoringUrlGenerator.generate()
      val keysDeleted = 1

      // when
      when(redisClient.del(url.id.toString)).thenReturn(keysDeleted)

      val result = monitoringService.getConfirmation(url.id)

      // then
      result must beEqualTo(MonitoringConfirmationResponse(url.id)).await

      verify(redisClient, times(1)).del(url.id.toString)
    }
  }

}
