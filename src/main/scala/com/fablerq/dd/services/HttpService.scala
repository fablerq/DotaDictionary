package com.fablerq.dd.services

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.repositories.{ ArticleRepository, VideoRepository, WordCollectionRepository, WordRepository }
import com.fablerq.dd.routes._

class HttpService {
  val wordRoutes = new WordRoutes(new WordService(new WordRepository))
  val wordCollectionRoutes = new WordCollectionRoutes(new WordCollectionService(new WordCollectionRepository))
  val videoRoutes = new VideoRoutes(new VideoService(new VideoRepository))
  val articleRoutes = new ArticleRoutes(new ArticleService(new ArticleRepository))
  val mainRoutes = new MainRoutes(new MainService)
  val routes =
    pathPrefix("api") {
      wordRoutes.route ~ wordCollectionRoutes.route ~ videoRoutes.route ~
        articleRoutes.route ~ mainRoutes.route
    }
}
