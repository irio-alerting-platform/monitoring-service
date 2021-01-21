package irio.alertingplatform.mailer

import java.util.Properties

import akka.actor.ActorSystem
import courier.{Envelope, Mailer, Text}
import irio.alertingplatform.mailer.MailerServiceConfig.MailerConfig
import irio.alertingplatform.utils.LoggingSupport
import javax.mail.Provider
import javax.mail.internet.InternetAddress
import org.jvnet.mock_javamail.{Mailbox, MockTransport}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import redis.clients.jedis.Jedis

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class MailerServiceSpec(implicit ee: ExecutionEnv)
    extends SpecificationLike
    with MockitoSugar
    with ArgumentMatchersSugar
    with LoggingSupport {

  implicit val system = ActorSystem("test")

  trait TestCase extends Scope {
    private val mockedSession = javax.mail.Session.getDefaultInstance(new Properties() {
      {
        put("mail.transport.protocol.rfc822", "mocked")
      }
    })
    mockedSession.setProvider(new MockedSMTPProvider)
    val mailerConfig = mock[MailerConfig]
    val jedis        = mock[Jedis]
    val mailerService = new MailerService(mailerConfig, jedis) {
      override def mailer: Mailer = Mailer(mockedSession)
    }
  }

  "mailer" should {
    "send any email" in new TestCase {
      // given
      val from    = "from@example.com"
      val to      = "to@example.com"
      val subject = "Test subject"
      val content = "Test content"

      // when
      val future = mailerService.mailer(
        Envelope
          .from(new InternetAddress(from))
          .to(new InternetAddress(to))
          .subject(subject)
          .content(Text(content))
      )

      // then
      Await.ready(future, 5.seconds)
      val toInbox = Mailbox.get(to)
      toInbox.size must beEqualTo(1)

      val toMsg = toInbox.get(0)
      toMsg.getSubject must beEqualTo(subject)
      toMsg.getContent must beEqualTo(content)
    }
  }

  private class MockedSMTPProvider
      extends Provider(Provider.Type.TRANSPORT, "mocked", classOf[MockTransport].getName, "Mock", null)

}
