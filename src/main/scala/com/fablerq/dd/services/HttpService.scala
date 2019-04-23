package com.fablerq.dd.services

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.repositories._
import com.fablerq.dd.routes._
import org.mongodb.scala.MongoDatabase

class HttpService(database: MongoDatabase) {

  //===============
  //Word
  //===============

  val wordService =
    new WordServiceImpl(
      new WordRepository(database.getCollection("words"))
    )

  val wordRoutes = new WordRoutes(wordService)

  //===============
  //WordCollection
  //===============

  val wordCollectionService =
    new WordCollectionServiceImpl(
      new WordCollectionRepository(
        database.getCollection("wordcollections")
      )
    )

  val wordCollectionRoutes = new WordCollectionRoutes(wordCollectionService)

  //===============
  //Video
  //===============

  val videoService =
    new VideoServiceImpl(
      new VideoRepository(database.getCollection("videos"))
    )

  val videoRoutes = new VideoRoutes(videoService)

  //===============
  //Article
  //===============

  val articleService =
    new ArticleServiceImpl(
      new ArticleRepository(database.getCollection("articles"))
    )

  val articleRoutes = new ArticleRoutes(articleService)

  //===============
  //MainService
  //===============

  val mainService = new MainServiceImpl(
    wordService,
    wordCollectionService,
    articleService,
    videoService
  )

  val mainRoutes = new MainRoutes(mainService)

  //===============
  //Quiz
  //===============

  val quizService =
    new QuizServiceImpl(
      new QuizRepository(database.getCollection("quizzes")),
      wordCollectionService,
      mainService,
      wordService
    )

  val quizRoutes = new QuizRoutes(quizService)


  val routes =
    pathPrefix("api") {
      wordRoutes.route ~ wordCollectionRoutes.route ~ videoRoutes.route ~
        articleRoutes.route ~ quizRoutes.route ~ mainRoutes.route
    }
}
