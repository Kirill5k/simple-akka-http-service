package io.kirill.boxofficeapp.events

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}

object TicketsSeller {
  case object GetEvent
  case object GetTickets
  case class CreateTickets(amount: Int)


  def props(event: Event) = Props(new TicketsSeller(event))
}

class TicketsSeller private (event: Event) extends Actor with ActorLogging {
  import TicketsSeller._
  import EventsManager._

  override def receive: Receive = withTickets(List(), 0)

  def withTickets(tickets: Seq[Ticket], latestTicketNumber: Int): Receive = {
    case GetEvent =>
      log.info("returning event")
      sender() ! Some(event)

    case GetTickets =>
      log.info(s"returning ${tickets.size} tickets")
      sender() ! Some(tickets)

    case CreateTickets(_) if LocalDateTime.now() isAfter event.date =>
      val message = s"trying to book tickets for passed event. original date ${event.date}"
      log.error(message)
      sender() ! CreateTicketsFailure(message)

    case CreateTickets(amount) if amount > event.seatsCount - tickets.size =>
      val message = s"not enough available seats: required - ${amount}, available - ${event.seatsCount - tickets.size}"
      log.error(message)
      sender() ! CreateTicketsFailure(message)

    case CreateTickets(amount) =>
      val newTickets = (latestTicketNumber until latestTicketNumber+amount).map{ number =>
        Ticket(UUID.randomUUID().toString, event.name, LocalDateTime.now(), number)
      }
      sender() ! CreateTicketsSuccess
      context.become(withTickets(tickets :+ newTickets, latestTicketNumber + amount))

    case message => log.info(s"received message: ${message}")
  }
}
