package com.fablerq.dd.routes

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.models.WordCollectionParams
import com.fablerq.dd.services.WordCollectionService
import com.fablerq.dd.configs.Json4sSupport._

class WordCollectionRoutes(wordCollectionService: WordCollectionService) {

  def route = {
    path("wordcollections") {
      get {
        complete(wordCollectionService.getAllWordCollections)
      } ~
        post {
          entity(as[WordCollectionParams]) { params =>
            complete(wordCollectionService.addWordCollection(params))
          }
        } ~
        patch {
          entity(as[WordCollectionParams]) { params =>
            complete(wordCollectionService.updateWordCollectionDescription(params))
          }
        } ~
        parameters("id".as[String]) { id =>
          delete {
            complete(wordCollectionService.deleteWordCollection(id))
          } ~
            post {
              complete(wordCollectionService.getWordCollection(id))
            }
        } ~
        parameters("collection".as[String], "word".as[String]) { (collection, word) =>
          delete {
            complete(wordCollectionService.deleteWordFromWordCollection(collection, word))
          } ~
            post {
              complete(wordCollectionService.addWordToWordCollection(collection, word))
            }
        }
    }
  }

}
