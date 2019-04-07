package com.fablerq.dd.routes

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.services.MainService
import com.fablerq.dd.configs.Json4sSupport._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

class MainRoutes(mainService: MainService) {

  def route = cors() {
    path("main") {
      parameters("request".as[String]) { request =>
        post {
          complete(mainService.defineRequest(request))
        }
      }
    }
  }

}
