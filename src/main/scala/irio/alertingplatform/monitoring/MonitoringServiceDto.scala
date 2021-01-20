package irio.alertingplatform.monitoring

object MonitoringServiceDto {

  case class MonitoringUrlsRequest(urls: List[MonitoringUrlDto], externalIp: String)

  case class MonitoringUrlDto(
    url: String,
    adminFst: String,
    adminSnd: String,
    frequencyMillis: Long,
    alertingWindow: Int,
    allowedResponseTimeMillis: Long
  )

  case class MonitoringUrlsResponse(urls: List[String])

}
