package com.fablerq.dd.routes

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.configs.Json4sSupport._
import com.fablerq.dd.models.ArticleParams
import com.fablerq.dd.services.ArticleService

class ArticleRoutes(articleService: ArticleService) {

  def route = {
    path("articles") {
      get {
        complete(articleService.getAllArticles)
      } ~
        post {
          entity(as[ArticleParams]) { params =>
            complete(articleService.addArticle(params))
          }
        } ~
        patch {
          entity(as[ArticleParams]) { params =>
            complete(articleService.updateArticleBody(params))
          }
        } ~
        parameters("id".as[String]) { id =>
          post {
            complete(articleService.getArticle(id))
          } ~
            delete {
              complete(articleService.deleteArticle(id))
            }
        } ~
        parameters("article".as[String], "stat".as[String]) { (article, stat) =>
          delete {
            complete(articleService.deleteStatFromArticle(article, stat))
          }
        } ~
        parameters("article".as[String], "stat".as[String], "percent".as[Int]) { (article, stat, percent) =>
          post {
            complete(articleService.addStatToArticle(article, stat, percent))
          }
        }
    }
  }

}
