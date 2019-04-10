package com.fablerq.dd.services

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.repositories.{ ArticleRepository, VideoRepository, WordCollectionRepository, WordRepository }
import com.fablerq.dd.routes._
import org.mongodb.scala.MongoDatabase

class HttpService(database: MongoDatabase) {

  val wordService =
    new WordServiceImpl(
      new WordRepository(database.getCollection("words"))
    )

  val wordRoutes =
    new WordRoutes(wordService)

  val wordCollectionService =
    new WordCollectionServiceImpl(
      new WordCollectionRepository(
        database.getCollection("wordcollections")
      )
    )

  val wordCollectionRoutes = new WordCollectionRoutes(wordCollectionService)

  val videoRoutes =
    new VideoRoutes(
      new VideoServiceImpl(
        new VideoRepository(database.getCollection("videos"))
      )
    )

  val articleService =
    new ArticleServiceImpl(
      new ArticleRepository(database.getCollection("articles"))
    )

  val articleRoutes = new ArticleRoutes(articleService)

  val mainService = new MainServiceImpl(wordService, wordCollectionService, articleService)

  val mainRoutes =
    new MainRoutes(mainService)

  val routes =
    pathPrefix("api") {
      wordRoutes.route ~ wordCollectionRoutes.route ~ videoRoutes.route ~
        articleRoutes.route ~ mainRoutes.route
    }
}
