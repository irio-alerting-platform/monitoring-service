package irio.alertingplatform.mailer

import akka.actor.ActorSystem
import courier.{Envelope, Mailer, Text}
import irio.alertingplatform.mailer.MailerServiceConfig.MailerConfig
import irio.alertingplatform.monitoring.MonitoringUrlGenerator
import irio.alertingplatform.utils.LoggingSupport
import javax.mail.internet.InternetAddress
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import redis.clients.jedis.Jedis

import scala.concurrent.Future

class MailerServiceSpec(implicit ee: ExecutionEnv)
    extends SpecificationLike
    with MockitoSugar
    with ArgumentMatchersSugar
    with LoggingSupport {

  implicit val system = ActorSystem("test")

  val mailerConfig =
    MailerConfig(
      host     = "example-host",
      port     = 587,
      from     = new InternetAddress("from@example.com"),
      pass     = "password1234",
      httpPort = 2137
    )

  trait TestCase extends Scope {
    val redisClient = mock[Jedis]
    val mockMailer  = mock[Mailer]
    val mailerService = new MailerService(mailerConfig, redisClient) {
      override def mailer: Mailer = mockMailer
    }
  }

  "sendMail" should {
    "send mail to first admin and insert mapping to redis" in new TestCase {
      // given
      val url              = MonitoringUrlGenerator.generate()
      val from             = mailerConfig.from
      val to               = url.adminFst
      val subject          = subjectString()
      val confirmationLink = confirmationLinkString(mailerConfig.httpPort, url.externalIp, url.id)
      val content          = contentString(url.url, confirmationLink)

      // when
      when(
        mockMailer(
          Envelope
            .from(from)
            .to(to)
            .subject(subject)
            .content(Text(content))
        )
      ).thenReturn(Future.successful())

      mailerService.sendMail(url)

      // then
      verify(redisClient, times(1)).set(any, any)
      verify(mockMailer, times(1))(
        Envelope
          .from(from)
          .to(to)
          .subject(subject)
          .content(Text(content))
      )
    }
  }

  "sendBackupMail" should {
    "send mail to second admin" in new TestCase {
      // given
      val url              = MonitoringUrlGenerator.generate()
      val from             = mailerConfig.from
      val to               = url.adminSnd
      val subject          = subjectString()
      val confirmationLink = confirmationLinkString(mailerConfig.httpPort, url.externalIp, url.id)
      val content          = contentString(url.url, confirmationLink)

      // when
      when(redisClient.get(url.id.toString)).thenReturn("1")

      when(
        mockMailer(
          Envelope
            .from(from)
            .to(to)
            .subject(subject)
            .content(Text(content))
        )
      ).thenReturn(Future.successful())

      mailerService.sendBackupMail(url)

      // then
      verify(redisClient, times(1)).get(url.id.toString)
      verify(mockMailer, times(1))(
        Envelope
          .from(from)
          .to(to)
          .subject(subject)
          .content(Text(content))
      )
    }
    "not send mail to second admin if mapping from redis was removed" in new TestCase {
      // given
      val url = MonitoringUrlGenerator.generate()

      // when
      when(redisClient.get(url.id.toString)).thenReturn(null)

      mailerService.sendBackupMail(url)

      // then
      verify(redisClient, times(1)).get(url.id.toString)
      verify(mockMailer, times(0))
    }
  }
}
