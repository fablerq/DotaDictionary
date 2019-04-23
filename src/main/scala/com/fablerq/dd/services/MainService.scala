package com.fablerq.dd.services

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.fablerq.dd.models._
import com.fablerq.dd.configs.Json4sSupport._
import net.liftweb.json.{JsonAST, _}

import sys.process._
import scala.io.Source
import java.nio.file.{Files, Paths}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.fablerq.dd.Server.system
import com.fablerq.dd.Server.materializer
import com.typesafe.config.ConfigFactory

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
  def handlingJson(request: String): Future[MainServiceResponse]
  def handlingDataFromJson(collectionTitle: String,
                           collectionDesc: String,
                           requestType: String,
                           jsonField: String,
                           isText: Boolean): Future[MainServiceResponse]
  def recordWord(word: String, repetitions: Long): Future[Boolean]
}

class MainServiceImpl(
    wordService: WordService,
    wordCollectionService: WordCollectionService,
    articleService: ArticleService,
    videoService: VideoService
) extends MainService {

  lazy val config = ConfigFactory.load()
  val yandexKey = config.getString("yandex.key")
  val aylienKey = config.getString("aylien.key")
  val aylienId = config.getString("aylien.id")

  val ValidURlRequest = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]".r
  val ValidWordRequest = "[a-zA-Z\\s]{0,20}".r
  val ValidYoutubeRequest = "^(https?\\:\\/\\/)?(www\\.youtube\\.com|youtu\\.?be)\\/.+$".r
  val ValidGithubJsonRequest = "https://raw.githubusercontent.com.*|.*json".r

  def defineRequest(request: String): Future[MainServiceResponse] = {
    request match {
      case x if x.matches(ValidYoutubeRequest.toString()) => handlingVideo(request)
      //case ValidGithubJsonRequest() => handlingJson(request)
      case ValidURlRequest(_) => handlingArticle(request)
      case ValidWordRequest() => handlingWord(request)
      case _ => Future.successful(MainServiceResponse(false))
    }
  }

  def setWords(text: String): List[WordStat] = {
    val initList: Map[String, Int] = text
      .replaceAll("[`*{}\\[\\]()>#+:~'%^&@<?;,\"!$=|./’\n\\d]", " ")
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
      .flatMap {
        case None =>
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
                      MainServiceResponse(
                        true,
                        Some("word"),
                        Some(typeId.get._id.toString))
                  }
                }
            }
        case Some(x) =>
          Future.successful(MainServiceResponse(
            true,
            Some("word"),
            Some(x._id.toString)
          ))
      }
  }

  def translateWord(word: String): Future[String] = {
    val formattedWord = word.replaceAll(" ", "%20")
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.GET,
        Uri(s"https://translate.yandex.net/api/v1.5/tr.json/translate" +
          s"?key=$yandexKey" +
          s"&text=$formattedWord" +
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
      .flatMap {
        case None =>
          Http()
            .singleRequest(HttpRequest(
              HttpMethods.GET,
              Uri(s"https://api.aylien.com/api/v1/extract?url=$articleLink")
            ).withHeaders(
                RawHeader("X-AYLIEN-TextAPI-Application-Key", aylienKey),
                RawHeader("X-AYLIEN-TextAPI-Application-ID", aylienId)
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
        case Some(article) =>
          Future.successful(MainServiceResponse(
          true,
          Some("article"),
          Some(article._id.toString)
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
      Future.successful(true)
    }
  }

  //=============================
  // Video
  //=============================

  def handlingVideo(videoLink: String): Future[MainServiceResponse] = {
    videoService.getIdByLink(videoLink)
      .flatMap {
        case None =>
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
        case Some(video) =>
         Future.successful(MainServiceResponse(
          true,
          Some("video"),
          Some(video._id.toString)
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
      Future.successful(true)
    }
  }

  //=============================
  // Json dota constants
  //=============================

  def handlingJson(request: String): Future[MainServiceResponse] = {
    request match {
      case x if x.endsWith("heroes.json") =>
        handlingDataFromJson(
          "Герои",
          "Коллекция с именами героев",
          "heroes",
          "localized_name",
          false).map { _ =>
            MainServiceResponse(true)
        }
      case x if x.endsWith("items.json") =>
        handlingDataFromJson(
          "Предметы",
          "Коллекция с названиями предметом",
          "items",
          "dname",
          false).flatMap { _ =>
            handlingDataFromJson(
              "Описания предметов",
              "Коллекция с описаниями предметов",
              "items",
              "lore",
              true).map { _ =>
                MainServiceResponse(true)
            }
        }
      case x if x.endsWith("abilities.json") =>
        handlingDataFromJson(
          "Способности",
          "Коллекция с названиями способностей героев",
          "abilities",
          "dname",
          false).flatMap { _ =>
          handlingDataFromJson(
            "Описания способностей",
            "Коллекция с описаниями способностей",
            "abilities",
            "desc",
            true).map { _ =>
            MainServiceResponse(true)
          }
        }
      case x if x.endsWith("hero_lore.json") =>
        handlingDataFromJson(
          "Лор героев",
          "Коллекция с именами героев",
          "hero_lore",
          "hero_lore",
          true).map { _ =>
          MainServiceResponse(true)
        }
      case _ =>
        Future.successful(MainServiceResponse(false))
    }
  }

  def handlingDataFromJson(collectionTitle: String,
                             collectionDesc: String,
                             requestType: String,
                             jsonField: String,
                             isText: Boolean): Future[MainServiceResponse] = {
    wordCollectionService.getWordCollectionByTitle(collectionTitle).flatMap {
      case Some(x) =>
        Future.successful(MainServiceResponse(true))
      case None =>
        val data: JValue =
          parse(Source.fromURL(s"https://raw.githubusercontent.com/odota/" +
            s"dotaconstants/master/build/$requestType.json").mkString)

        val extractedData: List[String] = jsonField match {
          case "hero_lore" =>
            for {
              JObject(x) <- data
              JField(_, JString(value)) <- x
            } yield value
          case _ =>
            for {
              JObject(x) <- data
              JField(field, JString(value)) <- x
              if field == jsonField
              if !value.exists(_.isDigit)
            } yield value
        }

        val purifiedData: Either[List[String], List[WordStat]] =
          if (isText) Right(setWords(extractedData.mkString))
          else Left(extractedData)

        val wordsForCollection: List[String] =
          purifiedData match {
            case Left(x) => x
            case Right(x) => x.map(x => x.word)
          }

        val newCollection =
          WordCollectionParams(
            Some(collectionTitle),
            Some(collectionDesc),
            Some(wordsForCollection))

        val translatedData: Future[List[Boolean]] =
          purifiedData match {
            case Left(x) =>
              Future.sequence(x.map { word =>
                recordWord(word, 1)
              })
            case Right(x) =>
              Future.sequence(x.map { wordStat =>
               recordWord(wordStat.word, wordStat.count)
              })
          }

          wordCollectionService.addWordCollection(newCollection).flatMap { x =>
            translatedData.map { _ =>
              if (x.bool)
                MainServiceResponse(true)
              else
                MainServiceResponse(false)
          }
        }
    }
  }

  def recordWord(word: String, repetitions: Long): Future[Boolean] = {
    wordService.getWordByTitle(word)
      .flatMap {
        case None =>
          translateWord(word).flatMap { translated =>
            val newWord = WordParams(
              Some(word),
              Some(translated),
              None,
              Some(repetitions))
            wordService.addWord(newWord).map(_ => true)
          }
        case Some(x) =>
          wordService.updateQuantity(x._id.toString, repetitions).map(_ => true)
      }
  }


}
