package irio.alertingplatform.mailer

import com.typesafe.config.Config
import javax.mail.internet.InternetAddress

object MailerServiceConfig {

  case class MailerConfig(host: String, port: Int, from: InternetAddress, pass: String)
  object MailerConfig {
    def apply(config: Config): MailerConfig = MailerConfig(
      host = config.getString("mailer.host"),
      port = config.getInt("mailer.port"),
      from = new InternetAddress(config.getString("mailer.from")),
      pass = config.getString("mailer.pass")
    )
  }

}
