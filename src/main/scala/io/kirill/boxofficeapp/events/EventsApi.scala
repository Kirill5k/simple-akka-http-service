package io.kirill.boxofficeapp.events

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import spray.json._
import scala.concurrent.ExecutionContext.Implicits.global


object EventsApi {
  case class CreateEventRequest(name: String, location: String, date: LocalDateTime, seatsCount: Int) {
    def toEvent(): Event = Event(name, location, date, seatsCount)
  }
  case class CreateEventResponse(name: String)
  case class ApiErrorResponse(message: String)
  case class GetEventResponse(name: String, location: String, date: LocalDateTime, seatsCount: Int)

  def apply(implicit system: ActorSystem, timeout: Timeout): EventsApi = new EventsApi()
}

trait EventsApiJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  import EventsApi._

  implicit object LocalDateTimeFormat extends JsonFormat[LocalDateTime] {
    def write(dateTime: LocalDateTime) = JsString(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    def read(value: JsValue) = value match {
      case JsString(dateTime) => LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      case _ => deserializationError("ISO date time formatted string expected.")
    }
  }

  implicit val createEventRequestFormat = jsonFormat4(CreateEventRequest)
  implicit val createEventResponseFormat = jsonFormat1(CreateEventResponse)
  implicit val getEventResponseFormat = jsonFormat4(GetEventResponse)
  implicit val apiErrorResponseFormat = jsonFormat1(ApiErrorResponse)
}

class EventsApi private (implicit system: ActorSystem, timeout: Timeout) extends EventsApiJsonSupport {
  import EventsManager._
  import EventsApi._

  val eventsManager = system.actorOf(EventsManager.props)
  val eventsRoute = pathPrefix("events") {
    path(Segment) { eventName =>
      path("tickets") {
        put {
          // buy tickets for event
          complete(StatusCodes.ServiceUnavailable)
        }
      } ~
      pathEndOrSingleSlash {
        get {
          val response = (eventsManager ? GetEventByName(eventName)).mapTo[Option[Event]].map {
            case Some(event) => StatusCodes.OK -> GetEventResponse(event.name, event.location, event.date, event.seatsCount)
            case None => StatusCodes.NotFound -> ApiErrorResponse(s"event ${eventName} does not exist")
          }
          complete(response)
        } ~
        delete {
          val response = (eventsManager ? DeleteEventByName(eventName)).map {
            case EventDeleted => StatusCodes.NoContent -> ""
            case EventNotFound => StatusCodes.NotFound -> ApiErrorResponse(s"event ${eventName} does not exist")
          }
          complete(response)
        }
      }
    } ~
    pathEndOrSingleSlash {
      get {
        val response = (eventsManager ? GetAllEvents).mapTo[List[Event]]
          .map(events => events.map(event => GetEventResponse(event.name, event.location, event.date, event.seatsCount)))
          .map(StatusCodes.OK -> _)

        complete(response)
      } ~
      post {
        entity(as[CreateEventRequest]) { request =>
          val response = (eventsManager ? CreateEvent(request.toEvent())).map {
            case EventCreated => StatusCodes.Created -> CreateEventResponse(request.name)
            case EventAlreadyExists => StatusCodes.Conflict -> ApiErrorResponse(s"event ${request.name} already exists")
          }
          complete(response)
        }
      }
    }
  }
}
