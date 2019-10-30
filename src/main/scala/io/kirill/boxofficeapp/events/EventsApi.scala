package io.kirill.boxofficeapp.events

import java.time.LocalDateTime

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.kirill.boxofficeapp.common.ApiJsonProtocol
import spray.json._

import scala.concurrent.ExecutionContext


object EventsApi {
  case class CreateEventRequest(name: String, location: String, date: LocalDateTime, seatsCount: Int) {
    def toEvent(): Event = Event(name, location, date, seatsCount)
  }
  case class CreateEventResponse(name: String)
  case class ApiErrorResponse(message: String)
  case class GetEventResponse(name: String, location: String, date: LocalDateTime, seatsCount: Int)

  def apply(implicit system: ActorSystem, ec: ExecutionContext, timeout: Timeout): EventsApi = {
    val eventsManager = system.actorOf(EventsManager.props)
    new EventsApi(eventsManager)
  }
}

trait EventsApiJsonProtocol extends ApiJsonProtocol {
  import EventsApi._

  implicit val createEventRequestFormat = jsonFormat4(CreateEventRequest)
  implicit val createEventResponseFormat = jsonFormat1(CreateEventResponse)
  implicit val getEventResponseFormat = jsonFormat4(GetEventResponse)
  implicit val apiErrorResponseFormat = jsonFormat1(ApiErrorResponse)
}

class EventsApi private (eventsManager: ActorRef) (implicit ec: ExecutionContext, timeout: Timeout) extends EventsApiJsonProtocol {
  import EventsManager._
  import EventsApi._

  val eventsRoute: Route = pathPrefix("events") {
    path(Segment) { eventName =>
      path("tickets") {
        get {
          // get all tickets for event
          complete(StatusCodes.ServiceUnavailable)
        } ~
        put {
          // buy tickets for event
          complete(StatusCodes.ServiceUnavailable)
        }
      } ~
      pathEndOrSingleSlash {
        get {
          val response = (eventsManager ? GetEventByName(eventName)).mapTo[Option[Event]].map {
            case Some(event) =>
              StatusCodes.OK -> GetEventResponse(event.name, event.location, event.date, event.seatsCount).toJson.prettyPrint
            case None =>
              StatusCodes.NotFound -> ApiErrorResponse(s"event ${eventName} does not exist").toJson.prettyPrint
          }
          complete(response)
        } ~
        delete {
          val response = (eventsManager ? DeleteEventByName(eventName)).map {
            case EventDeleted =>
              StatusCodes.NoContent -> ""
            case EventNotFound =>
              StatusCodes.NotFound -> ApiErrorResponse(s"event ${eventName} does not exist").toJson.prettyPrint
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
            case EventCreated => StatusCodes.Created ->
              CreateEventResponse(request.name).toJson.prettyPrint
            case EventAlreadyExists =>
              StatusCodes.Conflict -> ApiErrorResponse(s"event ${request.name} already exists").toJson.prettyPrint
          }
          complete(response)
        }
      }
    }
  }
}
