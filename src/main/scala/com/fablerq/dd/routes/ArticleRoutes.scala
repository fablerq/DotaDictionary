package com.fablerq.dd.routes

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.configs.Json4sSupport._
import com.fablerq.dd.models.ArticleParams
import com.fablerq.dd.services.ArticleService
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
class ArticleRoutes(articleService: ArticleService) {

  def route = cors() {
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
            complete(articleService.updateArticleLink(params))
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
        parameters("article".as[String], "stat".as[String], "percent".as[Int]) {
          (article, stat, percent) =>
            post {
              complete(articleService.addStatToArticle(article, stat, percent))
            }
        } ~
        parameters("article".as[String], "wordstat".as[String]) {
          (article, wordstat) =>
            delete {
              complete(articleService.deleteStatWordFromArticle(article, wordstat))
            } ~
              post {
                complete(articleService.addStatWordToArticle(article, wordstat))
              } ~
              patch {
                complete(articleService.updateWordStatForArticle(article, wordstat))
              }
        }
    } ~
    path("countarticles") {
      parameters("id".as[String], "page".as[Int]) {
        (id, page) =>
          post {
            complete(articleService.getWordsByPage(id, page))
          }
      } ~
      parameters("id".as[String]) {
        id =>
          post {
            complete(articleService.getCountOfWords(id))
          }
      }
    }
  }

}
