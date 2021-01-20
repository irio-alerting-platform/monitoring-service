package irio.alertingplatform.monitoring

import akka.actor.ActorSystem
import akka.testkit.TestKit
import irio.alertingplatform.mailer.MailerService
import irio.alertingplatform.monitoring.MonitoringServiceConfig.MonitoringConfig
import irio.alertingplatform.monitoring.MonitoringServiceDto.{MonitoringUrlDto, MonitoringUrlsRequest, MonitoringUrlsResponse}
import irio.alertingplatform.redis.MonitoringRedisClient
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope

class MonitoringServiceSpec(implicit ee: ExecutionEnv)
  extends TestKit(ActorSystem())
    with SpecificationLike
    with MockitoSugar
    with ArgumentMatchersSugar {

  trait TestCase extends Scope {
    val mailerService = mock[MailerService]
    val monitoringConfig = mock[MonitoringConfig]
    val redisClient = mock[MonitoringRedisClient]
    val monitoringService = new MonitoringService(monitoringConfig, mailerService, redisClient)
  }

  "getNewUrls" should {
    "return URLs received for monitoring" in new TestCase {
      val monitoringUrlDto = MonitoringUrlDto(
        url = "http://www.test.com/",
        frequencyMillis = 2000,
        alertingWindow = 3,
        adminFst = "admin1@domain.com",
        adminSnd = "admin2@domain.com",
        allowedResponseTimeMillis = 30000,
        externalIp = "0.0.0.0"
      )
      val urls = List(monitoringUrlDto)
      val request = MonitoringUrlsRequest(urls)

      val result = monitoringService.getUrlsToMonitor(request)

      result must beEqualTo(MonitoringUrlsResponse(urls.map(_.url))).await
    }
  }
}
