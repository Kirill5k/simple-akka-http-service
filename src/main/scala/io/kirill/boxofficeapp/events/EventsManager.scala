package io.kirill.boxofficeapp.events

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Future}

object EventsManager {
  case class CreateEvent(event: Event)
  case object EventCreated
  case object EventDeleted
  case object EventNotFound
  case object EventAlreadyExists
  case class GetEventByName(eventName: String)
  case class DeleteEventByName(eventName: String)
  case object GetAllEvents

  def props(implicit executionContext: ExecutionContext, timeout: Timeout) = Props(new EventsManager())
}

class EventsManager private (implicit ec: ExecutionContext, timeout: Timeout) extends Actor with ActorLogging {
  import EventsManager._
  import TicketsSeller._

  override def receive: Receive = {
    case CreateEvent(event) => context.child(event.name) match {
      case Some(_) =>
        log.info(s"event ${event.name} already exists")
        sender() ! EventAlreadyExists
      case None =>
        context.actorOf(TicketsSeller.props(event), event.name)
        log.info(s"created new event ${event.name}")
        sender() ! EventCreated
    }

    case GetEventByName(eventName) => context.child(eventName) match {
      case Some(ticketsSeller) =>
        log.info(s"retrieving event ${eventName}")
        ticketsSeller forward GetEvent
      case None =>
        log.info(s"event ${eventName} not found")
        sender() ! None
    }

    case DeleteEventByName(eventName) => context.child(eventName) match {
      case Some(ticketsSeller) =>
        log.info(s"removing event ${eventName}")
        ticketsSeller ! PoisonPill
        sender() ! EventDeleted
      case None =>
        log.info(s"event ${eventName} not found")
        sender() ! EventNotFound
    }

    case GetAllEvents =>
      log.info("retrieving all events")
      val seqOfFutureEvents = context.children.map(_ ? GetEvent).map(_.mapTo[Option[Event]])
      val futureEvents = Future.sequence(seqOfFutureEvents).map(_.flatten).map(_.toList)
      pipe(futureEvents) to sender()
  }
}
