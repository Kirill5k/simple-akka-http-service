package io.kirill.boxofficeapp

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global


object BoxOfficeApplication extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val defaultTimeout = Timeout(2 seconds)

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")
}
