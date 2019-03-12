package com.fablerq.dd.services

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.repositories.WordRepository
import com.fablerq.dd.routes.WordRoutes

class HttpService {
  val wordroutes = new WordRoutes(new WordService(new WordRepository))
  val routes =
    pathPrefix("api") {
      wordroutes.route
    }
}
