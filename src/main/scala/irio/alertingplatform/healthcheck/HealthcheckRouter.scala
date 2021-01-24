package irio.alertingplatform.healthcheck

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContext

class HealthcheckRouter(implicit ec: ExecutionContext) {
  def routes: Route = (path("healthz") & pathEndOrSingleSlash & get)(complete(OK))
}
