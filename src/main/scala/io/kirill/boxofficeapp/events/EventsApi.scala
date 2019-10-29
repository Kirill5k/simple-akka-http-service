package io.kirill.boxofficeapp.events

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import spray.json._


object EventsApi {
  def apply(implicit system: ActorSystem, timeout: Timeout): EventsApi = {
    val eventsManager = system.actorOf(EventsManager.props)
    new EventsApi(eventsManager, timeout)
  }
}

trait EventsApiJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {

}

class EventsApi private (eventsManager: ActorRef, timeout: Timeout) extends EventsApiJsonSupport {
  import EventsManager._

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
          // get event by name
          complete(StatusCodes.ServiceUnavailable)
        } ~
        delete {
          // cancel event by name
          complete(StatusCodes.ServiceUnavailable)
        }
      }
    } ~
    pathEndOrSingleSlash {
      get {
        // get all events
        complete(StatusCodes.ServiceUnavailable)
      } ~
      post {
        // create new event
        complete(StatusCodes.ServiceUnavailable)
      }
    }
  }
}
