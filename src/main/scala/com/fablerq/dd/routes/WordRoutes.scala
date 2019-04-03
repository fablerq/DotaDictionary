package com.fablerq.dd.routes

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.services.WordService
import com.fablerq.dd.configs.Json4sSupport._
import com.fablerq.dd.models.WordParams

class WordRoutes(wordService: WordService) {

  def route = {
    path("words") {
      get {
        complete(wordService.getAllWords)
      } ~
        post {
          entity(as[WordParams]) { params =>
            complete(wordService.addWord(params))
          }
        } ~
        patch {
          entity(as[WordParams]) { params =>
            complete(wordService.updateWordTranslate(params))
          }
        } ~
        parameters("id".as[String]) { id =>
          delete {
            complete(wordService.deleteWord(id))
          } ~
            patch {
              complete(wordService.updateQuantity(id))
            } ~
            post {
              complete(wordService.getWord(id))
            }
        }
    }
  }
}

