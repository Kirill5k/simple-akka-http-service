package io.kirill.boxofficeapp.events

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

trait DefaultTestSpec extends WordSpecLike with BeforeAndAfterAll { this: { val system: ActorSystem } =>
  implicit val timeout: Timeout = Timeout(2 seconds)

  override protected def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }
}
