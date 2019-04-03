package com.fablerq.dd.services

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.fablerq.dd.models._
import com.fablerq.dd.configs.Json4sSupport._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.fablerq.dd.Server.system
import com.fablerq.dd.Server.materializer
import com.fablerq.dd.repositories.{ ArticleRepository, WordRepository }

class MainService {

  val wordRepository = new WordRepository
  val wordService = new WordService(wordRepository)

  val articleRepository = new ArticleRepository
  val articleService = new ArticleService(articleRepository)

  def defineRequest(request: String): Future[MainServiceResponse] = {
    val validURlRequest = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]".r
    val validWordRequest = "[a-zA-Z]+".r
    val validYoutubeRequest = "^(http(s)??\\:\\/\\/)?(www\\.)?((youtube\\.com\\/watch\\?v=)|(youtu.be\\/))([a-zA-Z0-9\\-_])+".r

    request match {
      case validYoutubeRequest(_) => handlingVideo(request)
      case validURlRequest(_) => handlingArticle(request)
      case x if x.matches("[a-zA-Z]+") => handlingWord(request)
      case _ => Future(MainServiceResponse(false))
    }
  }

  def handlingWord(word: String): Future[MainServiceResponse] = {
    translateWord(word).map {
      translate =>
        wordService.addWord(WordParams(
          Some(word),
          Some(translate),
          None
        ))
    }.flatMap { x =>
      wordService.getWordByTitle(word).map {
        typeId => MainServiceResponse(true, Some("word"), Some(typeId._id.toString))
      }
    }
  }

  def handlingArticle(articleLink: String): Future[MainServiceResponse] = {
    val article: Future[Option[ArticleResponse]] = Http()
      .singleRequest(HttpRequest(
        HttpMethods.GET,
        Uri(s"https://api.aylien.com/api/v1/extract?url=$articleLink")
      ).withHeaders(
          RawHeader("X-AYLIEN-TextAPI-Application-Key", System.getenv("AYLIEN-KEY")),
          RawHeader("X-AYLIEN-TextAPI-Application-ID", System.getenv("AYLIEN-ID"))
        ))
      .flatMap(Unmarshal(_).to[Option[ArticleResponse]])

    article.map {
      x =>
        x.get.article
          .split(" ")
          .filter(x => x.length > 2)
          .map {
            x =>
              translateWord(x).map {
                y =>
                  wordService.addWord(WordParams(
                    Some(x),
                    Some(y),
                    None
                  ))
              }
          }

        articleService.addArticle(ArticleParams(
          Some(x.get.title),
          Some(x.get.article),
          Some(articleLink)
        ))
    }

    Future(MainServiceResponse(true))
  }

  def translateWord(word: String): Future[String] = {
    val translate: Future[Option[TranslateResponse]] = Http()
      .singleRequest(HttpRequest(
        HttpMethods.GET,
        Uri(s"https://translate.yandex.net/api/v1.5/tr.json/translate" +
          s"?key=trnsl.1.1.20190403T210630Z.b87c37e7300cf251.ede7f1fd18cf6c4d4648985834a9dc92818d6556" +
          s"&text=$word" +
          s"&lang=en-ru" +
          s"&format=plain")
      ))
      .flatMap(Unmarshal(_).to[Option[TranslateResponse]])
    translate.map {
      case x => x.get.text.head
    }
  }

  def handlingVideo(videoLink: String): Future[MainServiceResponse] = {
    ???
  }

}
