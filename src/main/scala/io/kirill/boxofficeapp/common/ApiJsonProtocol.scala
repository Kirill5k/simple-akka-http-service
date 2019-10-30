package io.kirill.boxofficeapp.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, deserializationError}

trait ApiJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object LocalDateTimeFormat extends JsonFormat[LocalDateTime] {
    def write(dateTime: LocalDateTime) = JsString(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    def read(value: JsValue) = value match {
      case JsString(dateTime) => LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      case _ => deserializationError("ISO date time formatted string expected.")
    }
  }
}
