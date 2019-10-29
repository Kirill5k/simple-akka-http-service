package io.kirill.simpleservice.events

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._

object EventsApi {

  def apply(implicit system: ActorSystem, timeout: Timeout): EventsApi = {
    val eventsManager = system.actorOf(Props[EventsManager])
    new EventsApi(eventsManager, timeout)
  }
}

class EventsApi private (eventsManager: ActorRef, timeout: Timeout) {

  val eventsRoute = pathPrefix("events") {
    path(Segment) { eventName =>
      path("tickets") {
        put {
          // buy tickets for event
        }
      } ~
      pathEndOrSingleSlash {
        get {
          // get event by name
        } ~
        delete {
          // delete event by name
        }
      }
    } ~
    pathEndOrSingleSlash {
      get {
        // get all events
      }~
      post {
        // create new event
      }
    }
  }
}
