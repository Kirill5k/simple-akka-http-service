package io.kirill.boxofficeapp.events

import java.time.LocalDateTime

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.kirill.boxofficeapp.common.DefaultResourceJsonProtocol
import spray.json._

import scala.concurrent.ExecutionContext


object EventsResource {
  case class CreateEventRequest(name: String, location: String, date: LocalDateTime, seatsCount: Int) {
    def toEvent(): Event = Event(name, location, date, seatsCount)
  }
  case class CreateEventResponse(name: String)
  case class GetEventResponse(name: String, location: String, date: LocalDateTime, seatsCount: Int)
  case class GetTicketResponse(id: String, purchaseDate: LocalDateTime, number: Int)
  case class CreateTicketsRequest(amount: Int)

  def apply(implicit system: ActorSystem, ec: ExecutionContext, timeout: Timeout): EventsResource = {
    val eventsManager = system.actorOf(EventsManager.props, "events-manager")
    new EventsResource(eventsManager)
  }
}

trait EventsResourceJsonProtocol extends DefaultResourceJsonProtocol {
  import EventsResource._

  implicit val createEventRequestFormat = jsonFormat4(CreateEventRequest)
  implicit val createEventResponseFormat = jsonFormat1(CreateEventResponse)
  implicit val getEventResponseFormat = jsonFormat4(GetEventResponse)
  implicit val getTicketResponseFormat = jsonFormat3(GetTicketResponse)
  implicit val createTicketsRequestFormat = jsonFormat1(CreateTicketsRequest)
}

class EventsResource (private val eventsManager: ActorRef)(implicit ec: ExecutionContext, timeout: Timeout) extends EventsResourceJsonProtocol {
  import EventsManager._
  import EventsResource._

  val eventsRoute: Route = pathPrefix("events") {
    path(Segment / "tickets") { eventName =>
      get {
        val response = (eventsManager ? GetTicketsForEvent(eventName)).mapTo[Option[List[Ticket]]].map {
          case Some(tickets) =>
            val ticketResponses = tickets.map(ticket => GetTicketResponse(ticket.id, ticket.purchaseDate, ticket.number))
            toJsonResponse(StatusCodes.OK, ticketResponses.toJson)
          case None =>
            toErrorResponse(StatusCodes.NotFound, s"event ${eventName} does not exist")
        }
        complete(response)
      } ~
      put {
        entity(as[CreateTicketsRequest]) { request =>
          val response = (eventsManager ? CreateTicketsForEvent(eventName, request.amount)).map {
            case CreateTicketsSuccess => HttpResponse(StatusCodes.Created)
            case CreateTicketsFailure(message) => toErrorResponse(StatusCodes.BadRequest, message)
          }
          complete(response)
        }
      }
    } ~
    path(Segment) { eventName =>
      get {
        val response = (eventsManager ? GetEventByName(eventName)).mapTo[Option[Event]].map {
          case Some(event) =>
            toJsonResponse(StatusCodes.OK, GetEventResponse(event.name, event.location, event.date, event.seatsCount).toJson)
          case None =>
            toErrorResponse(StatusCodes.NotFound, s"event ${eventName} does not exist")
        }
        complete(response)
      } ~
      delete {
        val response = (eventsManager ? CancelEventByName(eventName)).map {
          case CancelEventSuccess => HttpResponse(StatusCodes.NoContent)
          case EventNotFound => toErrorResponse(StatusCodes.NotFound, s"event ${eventName} does not exist")
        }
        complete(response)
      }
    } ~
    pathEndOrSingleSlash {
      get {
        val response = (eventsManager ? GetAllEvents).mapTo[List[Event]]
          .map(events => events.map(event => GetEventResponse(event.name, event.location, event.date, event.seatsCount)))
          .map(events => toJsonResponse(StatusCodes.OK, events.toJson))
        complete(response)
      } ~
      post {
        entity(as[CreateEventRequest]) { request =>
          val response = (eventsManager ? CreateEvent(request.toEvent())).map {
            case CreateEventSuccess =>
              toJsonResponse(StatusCodes.Created, CreateEventResponse(request.name).toJson)
            case EventAlreadyExists =>
              toErrorResponse(StatusCodes.Conflict, s"event ${request.name} already exists")
          }
          complete(response)
        }
      }
    }
  }
}
