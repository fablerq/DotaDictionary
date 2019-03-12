package com.fablerq.dd.routes

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.services.WordService
import com.fablerq.dd.configs.JsonSupport._

import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

class WordRoutes(wordService: WordService) {

  def route = cors() {
    pathPrefix("words") {

      (path("add") & post) {
        parameter('title, 'translate) { (title, translate) =>
          complete(wordService.addWord(title, translate))
        }

      } ~ (path("delete") & post) {
        parameter('title) { (title) =>
          complete(wordService.deleteWord(title))
        }

      } ~ (path("edit") & put) {
        parameter('title) { (title) =>
          complete(wordService.updateQuantity(title))
        }

      } ~ (path("detail") & get) {
        parameter('title) { (title) =>
          complete(wordService.getWord(title))
        }

      } ~ (pathEndOrSingleSlash & get) {
        complete(wordService.getAllWords)
      }
    }
  }
}
