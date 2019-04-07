package com.fablerq.dd

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.fablerq.dd.services.HttpService

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Properties, Success }
import com.fablerq.dd.configs.Mongo.database

object Server extends App {
  implicit val system: ActorSystem = ActorSystem("DD")
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val httpService = new HttpService(database)

  //heroku var
  val myPort = Properties.envOrElse("PORT", "8080").toInt

  Http().bindAndHandle(httpService.routes, "0.0.0.0", myPort).onComplete {
    case Success(b) => println(s"Server is running at ${b.localAddress.getHostName}:${b.localAddress.getPort}")
    case Failure(e) => println(s"Could not start application: {}", e.getMessage)
  }
}
