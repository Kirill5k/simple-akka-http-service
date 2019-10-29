package io.kirill.boxofficeapp.events

import com.github.nscala_time.time.Imports._

case class Ticket(id: String, eventName: String, purchaseDate: DateTime)

case class Event(name: String, location: String, date: DateTime, seatsCount: Int)
