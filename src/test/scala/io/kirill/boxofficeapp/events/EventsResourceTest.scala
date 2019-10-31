package io.kirill.boxofficeapp.events

import java.time.LocalDateTime

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestKit, TestProbe}
import akka.util.Timeout
import io.kirill.boxofficeapp.common.DefaultResourceJsonProtocol.ApiErrorResponse
import io.kirill.boxofficeapp.events.EventsManager.{AlreadyExists, Success}
import io.kirill.boxofficeapp.events.EventsResource._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.duration._
import scala.language.postfixOps

class EventsResourceTest extends WordSpec with BeforeAndAfterAll with Matchers with ScalatestRouteTest with EventsResourceJsonProtocol {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout: Timeout = Timeout(1 second)
  val eventsManagerProbe = TestProbe("em-probe")
  val eventsResource = new EventsResource(eventsManagerProbe.ref)

  val eventDate = LocalDateTime.now()
  val event1 = Event("ev1", "l1", eventDate, 100)
  val event2 = Event("ev2", "l2", eventDate, 200)

  "events resource" can {
    "get events" should {
      "return all events" in {
        Get("/events") ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.GetAll)
        eventsManagerProbe.reply(List(event1, event2))

        check {
          status shouldBe StatusCodes.OK
          responseAs[List[GetEventResponse]] shouldBe List(GetEventResponse("ev1", "l1", eventDate, 100), GetEventResponse("ev2", "l2", eventDate, 200))
        }
      }
    }

    "post events" should {
      val request = CreateEventRequest("new-event", "location", eventDate, 100)
      "create new event" in {
        Post("/events", request) ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.Create(Event("new-event", "location", eventDate, 100)))
        eventsManagerProbe.reply(Success)

        check {
          status shouldBe StatusCodes.Created
          responseAs[CreateEventResponse] shouldBe CreateEventResponse("new-event")
        }
      }

      "return 409 when event already exists" in {
        Post("/events", request) ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.Create(Event("new-event", "location", eventDate, 100)))
        eventsManagerProbe.reply(AlreadyExists)

        check {
          status shouldBe StatusCodes.Conflict
          responseAs[ApiErrorResponse] shouldBe ApiErrorResponse("event new-event already exists")
        }
      }
    }

    "get events/{eventName}" should {
      "return event by name" in {
        Get("/events/event-name") ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.GetByName("event-name"))
        eventsManagerProbe.reply(event1)

        check {
          status shouldBe StatusCodes.OK
          responseAs[List[GetEventResponse]] shouldBe GetEventResponse("ev1", "l1", event1.date, 100)
        }
      }

      "return 404 when requested event does not exist" in {
        Get("/events/event-name") ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.GetByName("event-name"))
        eventsManagerProbe.reply(EventsManager.NotFound)

        check {
          status shouldBe StatusCodes.NotFound
          responseAs[ApiErrorResponse] shouldBe ApiErrorResponse("event event-name does not exist")
        }
      }
    }

    "delete events/{eventName}" should {
      "cancel event by name" in {
        Delete("/events/event-name") ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.Cancel("event-name"))
        eventsManagerProbe.reply(Success)

        check {
          status shouldBe StatusCodes.NoContent
        }
      }

      "return 404 when cancelled event does not exist" in {
        Delete("/events/event-name") ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.Cancel("event-name"))
        eventsManagerProbe.reply(EventsManager.NotFound)

        check {
          status shouldBe StatusCodes.NotFound
          responseAs[ApiErrorResponse] shouldBe ApiErrorResponse("event event-name does not exist")
        }
      }
    }

    "get events/{eventName}tickets" should {
      "return tickets for an event" in {
        Get("/events/event-name/tickets") ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.GetTickets("event-name"))
        eventsManagerProbe.reply(List(Ticket("t1", "ev1", eventDate, 1), Ticket("t2", "ev1", eventDate, 2)))

        check {
          status shouldBe StatusCodes.OK
          responseAs[List[GetTicketResponse]] shouldBe List(GetTicketResponse("t1", eventDate, 1), GetTicketResponse("t2", eventDate, 2))
        }
      }

      "return 404 when requested event does not exist" in {
        Get("/events/event-name/tickets") ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.GetTickets("event-name"))
        eventsManagerProbe.reply(EventsManager.NotFound)

        check {
          status shouldBe StatusCodes.NotFound
          responseAs[ApiErrorResponse] shouldBe ApiErrorResponse("event event-name does not exist")
        }
      }
    }

    "put events/{eventName}tickets" should {
      val request = CreateTicketsRequest(100)
      "create new tickets" in {
        Put("/events/event-name/tickets", request) ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.CreateTickets("event-name", 100))
        eventsManagerProbe.reply(EventsManager.Success)

        check {
          status shouldBe StatusCodes.Created
        }
      }

      "return bad request when can't create tickets" in {
        Put("/events/event-name/tickets", request) ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.CreateTickets("event-name", 100))
        eventsManagerProbe.reply(EventsManager.BadRequest("invalid date"))

        check {
          status shouldBe StatusCodes.BadRequest
          responseAs[ApiErrorResponse] shouldBe ApiErrorResponse("invalid date")
        }
      }

      "return 404 when requested event does not exist" in {
        Put("/events/event-name/tickets", request) ~> eventsResource.eventsRoute ~> runRoute

        eventsManagerProbe.expectMsg(100 millis, EventsManager.CreateTickets("event-name", 100))
        eventsManagerProbe.reply(EventsManager.NotFound)

        check {
          status shouldBe StatusCodes.NotFound
          responseAs[ApiErrorResponse] shouldBe ApiErrorResponse("event event-name does not exist")
        }
      }
    }
  }
}
