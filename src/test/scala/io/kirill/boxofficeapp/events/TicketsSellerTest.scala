package io.kirill.boxofficeapp.events

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}


class TicketsSellerTest extends TestKit(ActorSystem("TicketsSellerTest")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val event = Event("ev1", "l1", LocalDateTime.now().plusDays(10), 100)
  val expiredEvent = Event("ev2", "l1", LocalDateTime.of(1990, 1, 1, 12, 0), 100)

  val eventsManager = TestProbe()
  val ticketsSeller = eventsManager.childActorOf(TicketsSeller.props(event))
  val expiredEventTicketsSeller = eventsManager.childActorOf(TicketsSeller.props(expiredEvent))

  "a tickets seller" can {
    "return its event" in {
      eventsManager.send(ticketsSeller, TicketsSeller.GetEvent)

      eventsManager.expectMsg(event)
    }

    "create tickets" should {
      "return empty list when no tickets created" in {
        eventsManager.send(ticketsSeller, TicketsSeller.GetTickets)

        eventsManager.expectMsg(List())
      }

      "return error when not enough seats available" in {
        eventsManager.send(ticketsSeller, TicketsSeller.CreateTickets(250))

        eventsManager.expectMsg(TicketsSeller.Failure("not enough available seats: required 250, available 100"))
      }

      "return error when trying to buy tickets for expired event" in {
        eventsManager.send(expiredEventTicketsSeller, TicketsSeller.CreateTickets(50))

        eventsManager.expectMsg(TicketsSeller.Failure("trying to book tickets for passed event. original date 1990-01-01T12:00"))
      }

      "return tickets after they were created" in {
        eventsManager.send(ticketsSeller, TicketsSeller.CreateTickets(2))
        eventsManager.expectMsg(TicketsSeller.Success)

        eventsManager.send(ticketsSeller, TicketsSeller.GetTickets)
        eventsManager.expectMsgPF() {
          case List(Ticket(_, "ev1", _, 0), Ticket(_, "ev1", _, 1)) => ()
        }
      }
    }
  }
}
