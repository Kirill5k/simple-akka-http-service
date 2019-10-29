package io.kirill.simpleservice.events

import java.time.LocalDateTime

case class Ticket(id: String, eventName: String)

case class Event(name: String, location: String, date: LocalDateTime, seatsCount: Int)
