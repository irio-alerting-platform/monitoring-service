package irio.alertingplatform.mailer

import javax.mail.internet.InternetAddress

object MailerServiceConfig {

  case class MailerConfig(
    host: String,
    port: Int,
    from: InternetAddress,
    pass: String,
    responseUrl: (String, Int) => String
  )
}
