package io.kirill.boxofficeapp.events

import akka.actor.{Actor, ActorLogging, Props}

object TicketsSeller {
  def props(event: Event) = Props(new TicketsSeller(event))
}

class TicketsSeller (event: Event) extends Actor with ActorLogging {
  override def receive: Receive = ???
}
