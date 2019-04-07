package com.fablerq.dd.services

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.repositories.{ ArticleRepository, VideoRepository, WordCollectionRepository, WordRepository }
import com.fablerq.dd.routes._
import org.mongodb.scala.MongoDatabase

class HttpService(database: MongoDatabase) {

  val wordRoutes =
    new WordRoutes(
      new WordService(
        new WordRepository(database.getCollection("words"))
      )
    )

  val wordCollectionRoutes =
    new WordCollectionRoutes(
      new WordCollectionService(
        new WordCollectionRepository(database.getCollection("wordcollections"))
      )
    )

  val videoRoutes =
    new VideoRoutes(
      new VideoService(
        new VideoRepository(database.getCollection("videos"))
      )
    )

  val articleRoutes =
    new ArticleRoutes(
      new ArticleService(
        new ArticleRepository(database.getCollection("articles"))
      )
    )

  val mainRoutes = new MainRoutes(new MainService)

  val routes =
    pathPrefix("api") {
      wordRoutes.route ~ wordCollectionRoutes.route ~ videoRoutes.route ~
        articleRoutes.route ~ mainRoutes.route
    }
}
