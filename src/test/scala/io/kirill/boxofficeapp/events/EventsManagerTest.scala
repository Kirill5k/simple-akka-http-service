package io.kirill.boxofficeapp.events

import java.time.LocalDateTime

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

class EventsManagerTest extends TestKit(ActorSystem("EventsManagerTest")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout: Timeout = Timeout(1 second)
  import system.dispatcher

  val eventDate = LocalDateTime.now()
  val event1 = Event("ev1", "l1", eventDate, 100)
  val event2 = Event("ev2", "l1", eventDate, 100)
  val ticket = Ticket("t1", "ev1", LocalDateTime.now(), 1)
  val ticketsSeller = TestProbe("ev1")

  "an event manager" can {

    "create new ticket seller" should {
      val eventsManager = createNewEventsManager(ticketsSeller, "ev1")
      "return success if events is new" in {
        eventsManager ! EventsManager.Create(event2)
        expectMsg(EventsManager.Success)
      }

      "return already exists if events is not new" in {
        eventsManager ! EventsManager.Create(event1)
        expectMsg(EventsManager.AlreadyExists)
      }
    }

    "get an event by name" should {
      val eventsManager = createNewEventsManager(ticketsSeller, "ev1")
      "return event from child if it exists" in {
        eventsManager ! EventsManager.GetByName("ev1")

        ticketsSeller.expectMsg(TicketsSeller.GetEvent)
        ticketsSeller.reply(event1)

        expectMsg(event1)
      }

      "return not found if event is new" in {
        eventsManager ! EventsManager.GetByName("ev2")

        expectMsg(EventsManager.NotFound)
      }
    }

    "cancel event by name" should {
      val eventsManager = createNewEventsManager(ticketsSeller, "ev1")
      "delete child" in {
        eventsManager ! EventsManager.Cancel("ev1")

        expectMsg(EventsManager.Success)
      }

      "return not found if event does not exist" in {
        eventsManager ! EventsManager.Cancel("ev2")

        expectMsg(EventsManager.NotFound)
      }
    }

    "get all events" should {
      val eventsManager = createNewEventsManager(ticketsSeller, "ev1")

      "return all events" in {
        eventsManager ! EventsManager.GetAll

        ticketsSeller.expectMsg(TicketsSeller.GetEvent)
        ticketsSeller.reply(event1)

        expectMsg(List(event1))
      }
    }

    "get tickets for an event" should {
      val eventsManager = createNewEventsManager(ticketsSeller, "ev1")
      "return event from child if it exists" in {
        eventsManager ! EventsManager.GetTickets("ev1")

        ticketsSeller.expectMsg(TicketsSeller.GetTickets)
        ticketsSeller.reply(List(ticket))

        expectMsg(List(ticket))
      }

      "return not found if event is new" in {
        eventsManager ! EventsManager.GetTickets("ev2")

        expectMsg(EventsManager.NotFound)
      }
    }

    "create tickets for an event" should {
      val eventsManager = createNewEventsManager(ticketsSeller, "ev1")
      "return success if no error" in {
        eventsManager ! EventsManager.CreateTickets("ev1", 100)

        ticketsSeller.expectMsg(TicketsSeller.CreateTickets(100))
        ticketsSeller.reply(TicketsSeller.Success)

        expectMsg(EventsManager.Success)
      }

      "return bad request if error" in {
        eventsManager ! EventsManager.CreateTickets("ev1", 100)

        ticketsSeller.expectMsg(TicketsSeller.CreateTickets(100))
        ticketsSeller.reply(TicketsSeller.Failure("error-message"))

        expectMsg(EventsManager.BadRequest("error-message"))
      }

      "return not found if event is new" in {
        eventsManager ! EventsManager.CreateTickets("ev2", 100)

        expectMsg(EventsManager.NotFound)
      }
    }
  }


  def createNewEventsManager(mockedChild: TestProbe, childName: String) = {
    TestActorRef(new EventsManager() {
      override def preStart(): Unit = context.actorOf(Props(new Forwarder(mockedChild.ref)), childName)
    })
  }

  class Forwarder(target: ActorRef) extends Actor {
    def receive = {
      case msg => target forward msg
    }
  }
}
