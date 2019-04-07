package com.fablerq.dd.services

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.fablerq.dd.models._
import com.fablerq.dd.configs.Json4sSupport._

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import com.fablerq.dd.Server.system
import com.fablerq.dd.Server.materializer
import com.fablerq.dd.repositories.{ ArticleRepository, WordCollectionRepository, WordRepository }

import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import com.fablerq.dd.configs.Mongo._

class MainService {

  val wordService =
    new WordService(
      new WordRepository(wordCollection)
    )

  val wordCollectionService =
    new WordCollectionService(
      new WordCollectionRepository(wordCollectionCollection)
    )

  val articleService =
    new ArticleService(
      new ArticleRepository(articleCollection)
    )

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

  //=============================
  // Words
  //=============================

  def handlingWord(word: String): Future[MainServiceResponse] = {
    wordService.getWordByTitle(word)
      .flatMap { x =>
        if (x.isInstanceOf[Word]) {
          Future(x)
        } else {
          translateWord(word)
            .flatMap { translate =>
              wordService.addWord(WordParams(
                Some(word),
                Some(translate),
                None
              ))
            }
        }
      }
      .flatMap { _ =>
        wordService.getWordByTitle(word).map {
          typeId =>
            MainServiceResponse(true, Some("word"), Some(typeId._id.toString))
        }
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
              articleService.addArticle(
                ArticleParams(
                  Some(articleService.setArticleTitle(articleResponse.get.title)),
                  Some(articleLink)
                )
              )
                .flatMap { _ =>
                  articleService.getIdByLink(articleLink)
                    .flatMap { article =>
                      setArticleWords(article, articleResponse.get.article)
                        .flatMap { updatedArticle =>
                          setStatsForArticle(updatedArticle)
                            .flatMap { _ =>
                              Future(MainServiceResponse(
                                true,
                                Some("article"),
                                Some(article._id.toString)
                              ))
                            }
                        }
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

  def setArticleWords(article: Article, text: String): Future[Article] = {
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

    articleService.addSomeStatsWordToArticle(article._id.toString, finalList)
      .map {
        case Right(x) => x
      }

    //    articleService.getIdByLink(article.link.toString)
    //      .flatMap { updatedArticle =>
    //        println("kek0 " + updatedArticle)
    //        Future(updatedArticle)
    //      }

    //translateWord(x).map { y =>
    //    wordService.addWord(WordParams(
    //      Some(x),
    //      Some(y),
    //      None))}}
  }

  def setStatsForArticle(article: Article): Future[Unit] = {
    wordCollectionService.getAllWordCollections.map {
      case Right(x) =>
        x.map { collection =>
          //init stat of this collection
          println("pidor " + collection)
          println("pidor2 " + article)
          articleService.addStatToArticle(
            article._id.toString,
            collection._id.toString,
            0
          )
            .flatMap { _ =>
              article.words.map { word =>
                wordService.getWordByTitle(word.word).map { wordData =>
                  if (collection.words.contains(wordData._id.toString)) {
                    articleService.updateStatToArticle(
                      article._id.toString,
                      collection._id.toString,
                      100 / collection.words.length
                    )
                  }
                }
              }
              Future(true)
            }
        }
    }
  }

  //=============================
  // Video
  //=============================

  def handlingVideo(videoLink: String): Future[MainServiceResponse] = {
    ???
  }

}
