package io.kirill.simpleservice.events

import akka.actor.{Actor, ActorLogging, Props}

object EventsManager {
  def props = Props[EventsManager]
}

class EventsManager extends Actor with ActorLogging {
  override def receive: Receive = ???
}
