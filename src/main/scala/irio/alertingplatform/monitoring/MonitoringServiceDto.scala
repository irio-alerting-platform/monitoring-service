package irio.alertingplatform.monitoring

import java.util.UUID

object MonitoringServiceDto {

  case class MonitoringUrlDto(
    url: String,
    adminFst: String,
    adminSnd: String,
    frequencyMillis: Long,
    alertingWindow: Int,
    allowedResponseTimeMillis: Long
  )

  case class MonitoringUrlsRequest(urls: List[MonitoringUrlDto], externalIp: String)

  case class MonitoringUrlsResponse(urls: List[String])

  case class MonitoringConfirmationResponse(id: UUID)

}
