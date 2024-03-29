package io.kirill.boxofficeapp.events

import java.time.LocalDateTime

case class Ticket(id: String, eventName: String, purchaseDate: LocalDateTime, number: Int)

case class Event(name: String, location: String, date: LocalDateTime, seatsCount: Int)
