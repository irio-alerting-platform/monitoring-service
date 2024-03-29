package irio.alertingplatform.monitoring

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import irio.alertingplatform.monitoring.MonitoringServiceDto.MonitoringUrlsRequest

import scala.concurrent.ExecutionContext

class MonitoringServiceRouter(monitoringService: MonitoringService)(implicit ec: ExecutionContext)
    extends MonitoringServiceJsonProtocol {
  def routes: Route =
    pathPrefix("monitoring") {
      (path("urls") & pathEndOrSingleSlash & post & entity(as[MonitoringUrlsRequest])) { request =>
        complete {
          monitoringService
            .getUrls(request)
            .map[ToResponseMarshallable](res => OK -> res)
        }
      } ~
        (path("confirmation" / JavaUUID) & pathEndOrSingleSlash & get) { id =>
          complete {
            monitoringService
              .getConfirmation(id)
              .map[ToResponseMarshallable](res => OK -> res)
          }
        }
    }
}
