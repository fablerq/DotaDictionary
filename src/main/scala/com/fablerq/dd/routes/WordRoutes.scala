package com.fablerq.dd.routes

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.services.WordService
import com.fablerq.dd.configs.Json4sSupport._
import com.fablerq.dd.models.WordParams
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default

class WordRoutes(wordService: WordService) {

  def route = cors() {
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
            complete(wordService.deleteWordByTitle(id))
          } ~
            post {
              complete(wordService.getWordByTitle(id))
            }
        } ~
        parameters("getbyid".as[String]) { id =>
          post {
            complete(wordService.getWord(id))
          }
        } ~
        parameters("page".as[Int]) { page =>
          post {
            complete(wordService.getWordsByPage(page))
          }
        }
    } ~
    path("countwords") {
      post {
        complete(wordService.getCountOfWords())
      }
    } ~
    path("checkaudio") {
      parameters("title".as[String]) { title =>
        post {
          complete(wordService.getAudio(title))
        }
      }
    } ~
    path("getaudio" / Segment) { title =>
      getFromFile(s"src/main/resources/$title.wav")
    }
  }
}

