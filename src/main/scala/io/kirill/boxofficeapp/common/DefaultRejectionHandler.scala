package io.kirill.boxofficeapp.common

import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.http.scaladsl.server.RejectionHandler

trait DefaultRejectionHandler extends DefaultResourceJsonProtocol {

  implicit val defaultRejectionHandler = RejectionHandler.default.mapRejectionResponse {
    case HttpResponse(status, _, ent: HttpEntity.Strict, _) =>
      val message = ent.data.utf8String.replaceAll("\\\n", " ")
      toErrorResponse(status, message)
    case x => x
  }
}
