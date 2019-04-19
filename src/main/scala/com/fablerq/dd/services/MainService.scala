package com.fablerq.dd.services

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.fablerq.dd.models._
import com.fablerq.dd.configs.Json4sSupport._
import net.liftweb.json._

import sys.process._
import scala.io.Source
import java.nio.file.{ Files, Paths }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.fablerq.dd.Server.system
import com.fablerq.dd.Server.materializer
import org.mongodb.scala.bson.ObjectId

trait MainService {
  def defineRequest(request: String): Future[MainServiceResponse]
  def setWords(text: String): List[WordStat]
  def handlingWord(word: String): Future[MainServiceResponse]
  def translateWord(word: String): Future[String]
  def handlingArticle(articleLink: String): Future[MainServiceResponse]
  def setStatsForArticle(articleId: ObjectId, rightWords: List[WordStat]): Future[Boolean]
  def handlingVideo(videoLink: String): Future[MainServiceResponse]
  def setStatsForVideo(videoId: ObjectId, rightWords: List[WordStat]): Future[Boolean]
}

class MainServiceImpl(
    wordService: WordService,
    wordCollectionService: WordCollectionService,
    articleService: ArticleService,
    videoService: VideoService
) extends MainService {

  def defineRequest(request: String): Future[MainServiceResponse] = {
    val ValidURlRequest = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]".r
    val ValidWordRequest = "[a-zA-Z]+".r
    val ValidYoutubeRequest = "^(https?\\:\\/\\/)?(www\\.youtube\\.com|youtu\\.?be)\\/.+$".r

    request match {
      case x if x.matches(ValidYoutubeRequest.toString()) => handlingVideo(request)
      case ValidURlRequest(_) => handlingArticle(request)
      case ValidWordRequest() => handlingWord(request)
      case _ => Future(MainServiceResponse(false))
    }
  }

  def setWords(text: String): List[WordStat] = {
    val initList: Map[String, Int] = text
      .replaceAll("[`*{}\\[\\]()>#+:~'%^&@<?;,\"!$=|./’]", " ")
      .replaceAll("\n", " ")
      .replaceAll("\n\n", " ")
      .replaceAll("\\d", " ")
      .toLowerCase()
      .split(" ")
      .filter(x => x.length > 3)
      .toList
      .groupBy(identity)
      .map { x =>
        (x._1, x._2.length)
      }

    val finalList: List[WordStat] =
      initList
        .map(x => WordStat(x._1, x._2))
        .toList
    finalList
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
              val words: List[WordStat] = setWords(articleResponse.get.article)
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

  def setStatsForArticle(articleId: ObjectId, rightWords: List[WordStat]): Future[Boolean] = {
    wordCollectionService.getAllWordCollections.map {
      case Right(x) =>
        x.map { collection =>
          val count: Int =
            if (collection.words.isEmpty) 1 else collection.words.length
          articleService.addStatToArticle(
            articleId.toString,
            collection._id.toString,
            rightWords.count(x => collection.words.contains(x.word))
              * 100 / count
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
    videoService.getIdByLink(videoLink)
      .flatMap { x =>
        if (x == null) {
          s"youtube-dl --write-info-json --skip-download $videoLink -o src/main/resources/info".!
          s"youtube-dl --write-auto-sub --sub-lang en --skip-download $videoLink -o src/main/resources/subtitles".!
          if (Files.exists(Paths.get("src/main/resources/subtitles.en.vtt"))) {
            val subtitlesFile: String =
              Source.fromFile("src/main/resources/subtitles.en.vtt")
                .mkString
                .substring(145)
                .replaceAll("\\<.*?\\>", "")
                .replaceAll("(?m)^[0-9].*", "")

            val videoText: List[WordStat] =
              setWords(subtitlesFile)
                //cause of triple repetitions in vtt format
                .filter(_.count % 3 == 0)
                .map(x => WordStat(x.word, x.count / 3))

            val videoInfoJson: JValue =
              parse(Source.fromFile("src/main/resources/info.info.json").mkString)

            //why List[()] not simple turple?
            val videoInfo: List[(String, String)] = for {
              JObject(x) <- videoInfoJson
              JField("title", JString(title)) <- x
              JField("description", JString(description)) <- x
            } yield (title, description)

            val video = Video(
              new ObjectId(),
              videoInfo.head._1,
              videoInfo.head._2,
              videoText,
              videoLink,
              List()
            )

            videoService.addVideoDirectly(video)
              .flatMap { _ =>
                setStatsForVideo(video._id, videoText)
                  .flatMap { _ =>
                    Future(MainServiceResponse(
                      true,
                      Some("video"),
                      Some(video._id.toString)
                    ))
                  }
              }
          } else
            Future(MainServiceResponse(false))
        } else Future(MainServiceResponse(
          true,
          Some("article"),
          Some(x._id.toString)
        ))
      }
  }

  def setStatsForVideo(videoId: ObjectId, rightWords: List[WordStat]): Future[Boolean] = {
    wordCollectionService.getAllWordCollections.map {
      case Right(x) =>
        x.map { collection =>
          val count: Int =
            if (collection.words.isEmpty) 1 else collection.words.length
          videoService.addStatToVideo(
            videoId.toString,
            collection._id.toString,
            rightWords.count(x => collection.words.contains(x.word))
              * 100 / count
          )
        }
    }.flatMap { _ =>
      Future(true)
    }
  }

}
