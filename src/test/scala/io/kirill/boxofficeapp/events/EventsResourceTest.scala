package io.kirill.boxofficeapp.events

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.{TestKit, TestProbe}
import akka.util.Timeout
import io.kirill.boxofficeapp.events.EventsManager.GetAll
import io.kirill.boxofficeapp.events.EventsResource.GetEventResponse
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers, WordSpec, WordSpecLike}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class EventsResourceTest extends WordSpec with BeforeAndAfterAll with Matchers with ScalatestRouteTest with EventsResourceJsonProtocol {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout: Timeout = Timeout(1 second)
  val eventsManagerProbe = TestProbe("em-probe")
  val eventsResource = new EventsResource(eventsManagerProbe.ref)

  val event1 = Event("ev1", "l1", LocalDateTime.now(), 100)
  val event2 = Event("ev2", "l2", LocalDateTime.now(), 200)

  "events resource" should {
    "returns all events" in {
      Get("/events") ~> eventsResource.eventsRoute ~> runRoute

      eventsManagerProbe.expectMsg(100 millis, GetAll)
      eventsManagerProbe.reply(List(event1, event2))

      check {
        status shouldBe StatusCodes.OK
        responseAs[List[GetEventResponse]] shouldBe List(GetEventResponse("ev1", "l1", event1.date, 100), GetEventResponse("ev2", "l2", event2.date, 200))
      }
    }
  }
}
