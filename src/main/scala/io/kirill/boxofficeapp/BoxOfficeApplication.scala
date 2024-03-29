package io.kirill.boxofficeapp

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.kirill.boxofficeapp.common.DefaultRejectionHandler
import io.kirill.boxofficeapp.events.EventsResource

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps


object BoxOfficeApplication extends App with DefaultRejectionHandler {
  implicit val system = ActorSystem("boxoffice-app")
  implicit val materializer = ActorMaterializer()
  implicit val defaultTimeout = Timeout(2 seconds)
  implicit val executor: ExecutionContext =  scala.concurrent.ExecutionContext.global

  val log = Logging(system.eventStream, "boxoffice-app")
  val eventsApi = EventsResource(system, executor, defaultTimeout)
  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  Http().bindAndHandle(eventsApi.eventsRoute, host, port)
}
