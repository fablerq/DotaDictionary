package com.fablerq.dd

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.fablerq.dd.services.{HttpService}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object Server extends App {
  implicit val system: ActorSystem = ActorSystem("DD")
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val httpService = new HttpService


  Http().bindAndHandle(httpService.routes, "localhost", 9010).onComplete {
    case Success(b) => println(s"Server is running at ${b.localAddress.getHostName}:${b.localAddress.getPort}")
    case Failure(e) => println(s"Could not start application: {}", e.getMessage)
  }
}
