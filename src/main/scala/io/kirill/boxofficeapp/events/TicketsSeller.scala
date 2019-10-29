package io.kirill.boxofficeapp.events

import akka.actor.{Actor, ActorLogging, Props}

object TicketsSeller {
  case object GetEvent
  case object GetTickets
  case class ReserveTickets(count: Int)

  def props(event: Event) = Props(new TicketsSeller(event))
}

class TicketsSeller (event: Event) extends Actor with ActorLogging {
  import TicketsSeller._

  override def receive: Receive = withTickets(List())

  def withTickets(tickets: Seq[Ticket]): Receive = {
    case GetEvent =>
      log.info("returning event")
      sender() ! Some(event)
    case message => log.info(s"received message: ${message}")
  }
}
