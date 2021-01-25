package irio.alertingplatform

import java.util.UUID

package object mailer {

  def subjectString() = "[ALERT] Alerting Platform: error count limit reached!"

  def confirmationLinkString(externalIp: String, urlId: UUID) =
    s"http://$externalIp/monitoring/confirmation/$urlId"

  def contentString(url: String, confirmationLink: String) =
    s"Service $url is down. If you've read this e-mail, confirm by going to $confirmationLink."

  def contentStringBackup(url: String) =
    s"Service $url is down. You're seeing this because the first admin didn't click the confirmation link."
}
