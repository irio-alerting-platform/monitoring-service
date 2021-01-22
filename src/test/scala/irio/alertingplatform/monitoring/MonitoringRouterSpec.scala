package irio.alertingplatform.monitoring

import java.util.UUID

import akka.http.scaladsl.testkit.Specs2RouteTest
import irio.alertingplatform.monitoring.MonitoringRunnable.MonitoringUrl
import irio.alertingplatform.monitoring.MonitoringServiceDto.{
  MonitoringConfirmationResponse,
  MonitoringUrlDto,
  MonitoringUrlsRequest,
  MonitoringUrlsResponse
}
import org.mockito.MockitoSugar.mock
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import redis.clients.jedis.Jedis

class MonitoringRouterSpec(implicit ee: ExecutionEnv)
    extends SpecificationLike
    with Specs2RouteTest
    with MonitoringServiceJsonProtocol {

  trait TestCase extends Scope {
    val monitoringSchedulerService = mock[MonitoringSchedulerService]
    val redisClient                = mock[Jedis]
    val monitoringService          = new MonitoringService(monitoringSchedulerService, redisClient)
    val monitoringRouter           = new MonitoringServiceRouter(monitoringService)
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

  "monitoring service" should {
    "return given url list" in new TestCase {
      val externalIp    = "0.0.0.0"
      val urls          = List.fill(5)(MonitoringUrlGenerator.generate(externalIp = externalIp))
      val requestEntity = generateRequest(urls.map(generateDto), externalIp)

      val request = Post(s"/monitoring/urls", requestEntity)

      request ~> monitoringRouter.routes ~> check {
        responseAs[MonitoringUrlsResponse] shouldEqual MonitoringUrlsResponse(urls.map(_.url))
      }
    }
    "return id if confirmation link is clicked" in new TestCase {
      val uuid    = UUID.randomUUID()
      val request = Get(s"/monitoring/confirmation/$uuid")

      request ~> monitoringRouter.routes ~> check {
        responseAs[MonitoringConfirmationResponse] shouldEqual MonitoringConfirmationResponse(uuid)
      }
    }
  }

}
