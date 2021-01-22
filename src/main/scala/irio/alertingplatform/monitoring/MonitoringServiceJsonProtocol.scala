package irio.alertingplatform.monitoring

import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import irio.alertingplatform.monitoring.MonitoringServiceDto.{
  MonitoringConfirmationResponse,
  MonitoringUrlDto,
  MonitoringUrlsRequest,
  MonitoringUrlsResponse
}
import spray.json._

trait MonitoringServiceJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object UUIDFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)
    def read(value: JsValue): UUID =
      value match {
        case JsString(uuid) => UUID.fromString(uuid)
        case _              => throw DeserializationException("Expected UUID string")
      }
  }

  implicit val monitoringUrlDto: RootJsonFormat[MonitoringUrlDto]             = jsonFormat6(MonitoringUrlDto)
  implicit val monitoringUrlsRequest: RootJsonFormat[MonitoringUrlsRequest]   = jsonFormat2(MonitoringUrlsRequest)
  implicit val monitoringUrlsResponse: RootJsonFormat[MonitoringUrlsResponse] = jsonFormat1(MonitoringUrlsResponse)
  implicit val monitoringConfirmationResponse: RootJsonFormat[MonitoringConfirmationResponse] = jsonFormat1(
    MonitoringConfirmationResponse
  )
}
