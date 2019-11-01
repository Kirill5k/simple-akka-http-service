package io.kirill.boxofficeapp.events

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

object EventsManager {
  case object GetAll
  case class Create(event: Event)
  case class GetByName(eventName: String)
  case class Cancel(eventName: String)
  case class GetTickets(eventName: String)
  case class CreateTickets(eventName: String, amount: Int)

  case object Success
  case class BadRequest(message: String)
  case object AlreadyExists
  case object NotFound

  def props(implicit executionContext: ExecutionContext, timeout: Timeout) = Props(new EventsManager())
}

class EventsManager (implicit ec: ExecutionContext, timeout: Timeout) extends Actor with ActorLogging {
  import EventsManager._

  private def findTicketsSeller(eventName: String, originalSender: ActorRef)(f: ActorRef => Unit): Unit = {
    context.child(eventName) match {
      case Some(ticketsSeller) => f(ticketsSeller)
      case None =>
        log.error(s"event ${eventName} not found")
        originalSender ! NotFound
    }
  }

  override def receive: Receive = {
    case Create(event) => context.child(event.name) match {
      case Some(_) =>
        log.info(s"event ${event.name} already exists")
        sender() ! AlreadyExists
      case None =>
        context.actorOf(TicketsSeller.props(event), event.name)
        log.info(s"created new event ${event.name}")
        sender() ! Success
    }

    case GetByName(eventName) => findTicketsSeller(eventName, sender()) { ticketsSeller =>
      log.info(s"retrieving event ${eventName}")
      val futureEvent = (ticketsSeller ? TicketsSeller.GetEvent).mapTo[Event]
      pipe(futureEvent) to sender()
    }

    case Cancel(eventName) => findTicketsSeller(eventName, sender()) { ticketsSeller =>
        log.info(s"cancelling event ${eventName}")
        ticketsSeller ! PoisonPill
        sender() ! Success
    }

    case GetAll =>
      log.info("retrieving all events")
      val seqOfFutureEvents = context.children.map(_ ? TicketsSeller.GetEvent).map(_.mapTo[Event])
      val futureEvents = Future.sequence(seqOfFutureEvents).map(_.toList)
      pipe(futureEvents) to sender()

    case GetTickets(eventName) => findTicketsSeller(eventName, sender()) { ticketsSeller =>
        log.info(s"getting tickets for event ${eventName}")
        val futureTickets = (ticketsSeller ? TicketsSeller.GetTickets).mapTo[List[Ticket]]
        pipe(futureTickets) to sender()
    }

    case CreateTickets(eventName, amount) => findTicketsSeller(eventName, sender()) { ticketsSeller =>
        log.info(s"requesting to create ${amount} tickets for event ${eventName}")
        val futureResponse = (ticketsSeller ? TicketsSeller.CreateTickets(amount)).map {
          case TicketsSeller.Success => Success
          case TicketsSeller.Failure(message) => BadRequest(message)
        }
        pipe(futureResponse) to sender()
    }
  }
}
