package io.kirill.boxofficeapp.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCode}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, deserializationError}
import spray.json._

object ApiJsonProtocol {
  case class ApiErrorResponse(message: String)
}

trait ApiJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  import  ApiJsonProtocol._

  implicit object LocalDateTimeFormat extends JsonFormat[LocalDateTime] {
    def write(dateTime: LocalDateTime) = JsString(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    def read(value: JsValue) = value match {
      case JsString(dateTime) => LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      case _ => deserializationError("ISO date time formatted string expected.")
    }
  }

  implicit val apiErrorResponseFormat = jsonFormat1(ApiErrorResponse)

  protected def toJsonResponse(status: StatusCode, body: JsValue): HttpResponse =
    HttpResponse(status, entity = HttpEntity(ContentTypes.`application/json`, body.prettyPrint))

  protected def toErrorResponse(status: StatusCode, message: String): HttpResponse = {
    val responseBody = ApiErrorResponse(message).toJson.prettyPrint
    HttpResponse(status, entity = HttpEntity(ContentTypes.`application/json`, responseBody))
  }
}
