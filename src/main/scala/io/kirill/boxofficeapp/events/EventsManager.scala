package io.kirill.boxofficeapp.events

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object EventsManager {
  case object GetAllEvents
  case class CreateEvent(event: Event)
  case object CreateEventSuccess
  case object EventAlreadyExists
  case object EventDeleted
  case object EventNotFound
  case class GetEventByName(eventName: String)
  case class CancelEventByName(eventName: String)
  case class GetTicketsForEvent(eventName: String)
  case class CreateTicketsForEvent(eventName: String, amount: Int)
  case object CreateTicketsSuccess
  case class CreateTicketsFailure(message: String)

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
        sender() ! CreateEventSuccess
    }

    case GetEventByName(eventName) => context.child(eventName) match {
      case Some(ticketsSeller) =>
        log.info(s"retrieving event ${eventName}")
        ticketsSeller forward GetEvent
      case None =>
        log.info(s"event ${eventName} not found")
        sender() ! None
    }

    case CancelEventByName(eventName) => context.child(eventName) match {
      case Some(ticketsSeller) =>
        log.info(s"cancelling event ${eventName}")
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

    case GetTicketsForEvent(eventName) => context.child(eventName) match {
      case Some(ticketsSeller) =>
        log.info(s"getting tickets for event ${eventName}")
        ticketsSeller forward GetTickets
      case None =>
        log.info(s"event ${eventName} not found")
        sender() ! None
    }

    case CreateTicketsForEvent(eventName, amount) => context.child(eventName) match {
      case Some(ticketsSeller) =>
        log.info(s"requesting to create ${amount} tickets for event ${eventName}")
        ticketsSeller forward CreateTickets(amount)
      case None =>
        log.info(s"event ${eventName} not found")
        sender() ! EventNotFound
    }
  }
}
