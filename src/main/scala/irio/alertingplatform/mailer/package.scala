package irio.alertingplatform

import java.util.UUID

package object mailer {

  def subjectString() = "Alerting platform – services are down!"

  def confirmationLinkString(externalIp: String, urlId: UUID) =
    s"http://$externalIp/monitoring/confirmation/$urlId"

  def contentString(url: String, confirmationLink: String) =
    s"Service $url is down. If you've read this e-mail, confirm by going to $confirmationLink."

}
