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
import org.mongodb.scala.bson.ObjectId

trait MainService {
  def defineRequest(request: String): Future[MainServiceResponse]
  def handlingWord(word: String): Future[MainServiceResponse]
  def translateWord(word: String): Future[String]
  def handlingArticle(articleLink: String): Future[MainServiceResponse]
  def calcRepeating(list: List[String], map: Map[String, Int]): Map[String, Int]
  def setArticleWords(text: String): List[WordStat]
  def setStatsForArticle(articleId: ObjectId, rightWords: List[WordStat]): Future[Boolean]
  def handlingVideo(videoLink: String): Future[MainServiceResponse]
}

class MainServiceImpl(
    wordService: WordService,
    wordCollectionService: WordCollectionService,
    articleService: ArticleService
) extends MainService {

  def defineRequest(request: String): Future[MainServiceResponse] = {
    val ValidURlRequest = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]".r
    val ValidWordRequest = "[a-zA-Z]+".r
    val ValidYoutubeRequest = "^(http(s)??\\:\\/\\/)?(www\\.)?((youtube\\.com\\/watch\\?v=)|(youtu.be\\/))([a-zA-Z0-9\\-_])+".r

    request match {
      case ValidYoutubeRequest(_) => handlingVideo(request)
      case ValidURlRequest(_) => handlingArticle(request)
      case ValidWordRequest() => handlingWord(request)
      case _ => Future(MainServiceResponse(false))
    }
  }

  //=============================
  // Words
  //=============================

  def handlingWord(word: String): Future[MainServiceResponse] = {
    wordService.getWordByTitle(word)
      .flatMap { x =>
        if (x == null) {
          translateWord(word)
            .flatMap { translate =>
              wordService.addWord(WordParams(
                Some(word),
                Some(translate),
                None
              ))
                .flatMap { _ =>
                  wordService.getWordByTitle(word).map {
                    typeId =>
                      MainServiceResponse(true, Some("word"), Some(typeId._id.toString))
                  }
                }
            }
        } else Future(MainServiceResponse(
          true,
          Some("word"),
          Some(x._id.toString)
        ))
      }
  }

  def translateWord(word: String): Future[String] = {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.GET,
        Uri(s"https://translate.yandex.net/api/v1.5/tr.json/translate" +
          s"?key=trnsl.1.1.20190403T210630Z.b87c37e7300cf251.ede7f1fd18cf6c4d4648985834a9dc92818d6556" +
          s"&text=$word" +
          s"&lang=en-ru" +
          s"&format=plain")
      ))
      .flatMap(Unmarshal(_).to[Option[TranslateResponse]])
      .map(x => x.get.text.head)
  }

  //=============================
  // Articles
  //=============================

  def handlingArticle(articleLink: String): Future[MainServiceResponse] = {
    articleService.getIdByLink(articleLink)
      .flatMap { x =>
        if (x == null) {
          Http()
            .singleRequest(HttpRequest(
              HttpMethods.GET,
              Uri(s"https://api.aylien.com/api/v1/extract?url=$articleLink")
            ).withHeaders(
                RawHeader("X-AYLIEN-TextAPI-Application-Key", "60eb56a0e75d1bdd6eb7d616c7c721bb"),
                RawHeader("X-AYLIEN-TextAPI-Application-ID", "e8323052")
              ))
            .flatMap(Unmarshal(_).to[Option[ArticleResponse]])
            .flatMap { articleResponse =>
              val words: List[WordStat] = setArticleWords(articleResponse.get.article)
              val article = Article(
                new ObjectId(),
                articleService.setArticleTitle(articleResponse.get.title),
                words,
                articleLink,
                List()
              )
              articleService.addArticleDirectly(article)
                .flatMap { _ =>
                  setStatsForArticle(article._id, words)
                    .flatMap { _ =>
                      Future(MainServiceResponse(
                        true,
                        Some("article"),
                        Some(article._id.toString)
                      ))
                    }
                }
            }
        } else Future(MainServiceResponse(
          true,
          Some("article"),
          Some(x._id.toString)
        ))
      }
  }

  def calcRepeating(list: List[String], map: Map[String, Int]): Map[String, Int] =
    list match {
      case x :: y =>
        if (map.keySet.contains(x)) calcRepeating(y, map ++ Map(x -> (map(x) + 1)))
        else calcRepeating(y, map ++ Map(x -> 1))
      case Nil => map
    }

  def setArticleWords(text: String): List[WordStat] = {
    val initList = text
      .replaceAll("[`*{}\\[\\]()>#+:~'%^&@<?;,\"!$=|./’]", " ")
      .replaceAll("\n", " ")
      .replaceAll("\n\n", " ")
      .replaceAll("\\d", " ")
      .toLowerCase()
      .split(" ")
      .filter(x => x.length > 3)
      .toList
    val finalMap: Map[String, Int] = calcRepeating(initList, Map.empty)
    val finalList: List[WordStat] =
      finalMap
        .map(x => WordStat(x._1, x._2))
        .toList
    finalList
  }

  def setStatsForArticle(articleId: ObjectId, rightWords: List[WordStat]): Future[Boolean] = {
    wordCollectionService.getAllWordCollections.map {
      case Right(x) =>
        x.map { collection =>
          articleService.addStatToArticle(
            articleId.toString,
            collection._id.toString,
            rightWords.count(x => collection.words.contains(x.word))
              * 100 / collection.words.length
          )
        }
    }.flatMap { _ =>
      Future(true)
    }
  }

  //=============================
  // Video
  //=============================

  def handlingVideo(videoLink: String): Future[MainServiceResponse] = {
    Future(MainServiceResponse(
      true,
      Some("video"),
      Some("not ready")
    ))
  }

}
