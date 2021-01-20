package irio.alertingplatform.monitoring

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import irio.alertingplatform.monitoring.MonitoringServiceDto.{
  MonitoringUrlDto,
  MonitoringUrlsRequest,
  MonitoringUrlsResponse
}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait MonitoringServiceJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val monitoringUrlDtoFormat: RootJsonFormat[MonitoringUrlDto]         = jsonFormat6(MonitoringUrlDto)
  implicit val monitoringRequestFormat: RootJsonFormat[MonitoringUrlsRequest]   = jsonFormat2(MonitoringUrlsRequest)
  implicit val monitoringResponseFormat: RootJsonFormat[MonitoringUrlsResponse] = jsonFormat1(MonitoringUrlsResponse)
}
