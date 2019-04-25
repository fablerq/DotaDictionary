package com.fablerq.dd.services

import java.io.File
import java.nio.file.{Files, Paths}

import akka.http.javadsl.model.headers.ContentDisposition
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.stream.scaladsl._
import akka.stream.{Graph, Materializer, SinkShape}
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.Try
import akka.http.javadsl.model.headers.ContentDisposition
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.fablerq.dd.models._
import com.fablerq.dd.repositories.WordRepository
import org.bson.types.ObjectId
import org.mongodb.scala.bson.ObjectId
import com.fablerq.dd.Server.system
import com.fablerq.dd.Server.materializer
import com.typesafe.config.ConfigFactory
import org.json4s.jackson.Serialization.write
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.FileIO
import com.fablerq.dd.configs.Json4sSupport._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait WordService {
  def getAllWords: Future[Either[ServiceResponse, Seq[Word]]]
  def getWordsByPage(page: Int): Future[Either[ServiceResponse, Seq[Word]]]
  def getCountOfWords(): Future[ServiceResponse]
  def getWord(id: String): Future[Either[ServiceResponse, Word]]
  def getWordByTitle(title: String): Future[Option[Word]]
  def getByIdDirectly(id: ObjectId): Future[Option[Word]]
  def addWord(params: WordParams): Future[ServiceResponse]
  def updateWordTranslate(params: WordParams): Future[ServiceResponse]
  def deleteWord(id: String): Future[ServiceResponse]
  def deleteWordByTitle(title: String): Future[ServiceResponse]
  def updateQuantity(id: String, quantity: Long): Future[ServiceResponse]
  def getAudio(word: String): Future[ServiceResponse]
}

class WordServiceImpl(wordRepository: WordRepository) extends WordService {

  lazy val config = ConfigFactory.load()
  val ibmKey = config.getString("ibm.key")

  def getAllWords: Future[Either[ServiceResponse, Seq[Word]]] = {
    wordRepository.getAll.map {
      case x: Seq[Word] if x.nonEmpty => Right(x.sortBy(-_.quantity))
      case _ =>
        Left(ServiceResponse(false, "База данных слов пуста"))
    }
  }

  //to-do change this shit sorting
  def getWordsByPage(page: Int): Future[Either[ServiceResponse, Seq[Word]]] = {
    wordRepository.getAll.map {
      case x: Seq[Word] if x.nonEmpty =>
        Right(x
          .sortBy(-_.quantity)
          .slice((page - 1) * 15, page * 15))
      case _ =>
        Left(ServiceResponse(false, "База данных слов пуста"))
    }
  }

  def getCountOfWords(): Future[ServiceResponse] = {
    wordRepository.count.map{ x =>
      ServiceResponse(true, x.toString)
    }
  }

  def getWord(id: String): Future[Either[ServiceResponse, Word]] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      wordRepository.getById(objectId).map {
        case word: Word => Right(word)
        case _ =>
          Left(ServiceResponse(false, "Слово не найдено!"))
      }
    } else Future(Left(ServiceResponse(false, "Неверный запрос!")))
  }

  def getWordByTitle(title: String): Future[Option[Word]] = {
    wordRepository.getByTitle(title).map { x =>
      Option(x)
    }
  }

  def getByIdDirectly(id: ObjectId): Future[Option[Word]] =
    wordRepository.getById(id).map { x =>
      Option(x)
    }

  def addWord(params: WordParams): Future[ServiceResponse] = params match {
    case WordParams(Some(title), Some(translate), None, None) =>
      getWordByTitle(title).flatMap {
        case Some(x) =>
          Future.successful(
            ServiceResponse(false, s"Слово ${params.title} уже создано")
          )
        case None =>
          wordRepository.addWord(Word.apply(
            new ObjectId(), title,
            translate, 0, 1
          )).map { x =>
            ServiceResponse(true, "Слово успешно добавлено")
          }
      }
    //uses when adding word by json request in MainService
    case WordParams(Some(title), Some(translate), None, Some(quantity)) =>
      wordRepository.addWord(Word.apply(
        new ObjectId(), title,
        translate, 0, quantity
      )).map { x =>
        ServiceResponse(true, "Слово успешно добавлено")
      }
    case _ =>
      Future.successful(
        ServiceResponse(false,
          s"Слово ${params.title} не удалось добавить.Неверный запрос"))
  }

  def updateWordTranslate(params: WordParams): Future[ServiceResponse] = params match {
    case WordParams(Some(title), Some(translate), None, None) =>
      getWordByTitle(title).flatMap {
        case Some(x) =>
          wordRepository.updateWordTranslate(x._id, translate).map { x=>
            ServiceResponse(true, "Перевод слова успешно изменен")
          }
        case None =>
          Future.successful(ServiceResponse(false, s"Слово $title не найдено"))
      }
    case _ =>
      Future.successful(
        ServiceResponse(false,
          s"Слово ${params.title} не удалось обновить. Неверный запрос"))
  }

  def deleteWord(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          wordRepository.deleteWord(x._id).map { x =>
            ServiceResponse(true, "Слово успешно удалено")
          }
        case None =>
          Future.successful(ServiceResponse(false, "Не удалось удалить слово"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

  def deleteWordByTitle(title: String): Future[ServiceResponse] = {
    getWordByTitle(title).flatMap {
      case Some(x) =>
        wordRepository.deleteWord(x._id).map { x =>
          ServiceResponse(true, "Слово успешно удалено")
        }
      case None =>
        Future.successful(ServiceResponse(false, "Слова не существует"))
    }
  }

  def updateQuantity(id: String, quantity: Long): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      getByIdDirectly(objectId).flatMap {
        case Some(word) =>
          wordRepository.updateQuantity(
            word._id,
            word.quantity + quantity).map { x =>
            ServiceResponse(true, "Количество повторений у слова обновлено")
          }
        case None =>
          Future.successful(
            ServiceResponse(false,
              "Количество повторений у слова не удалось обновить"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }


  //to-do change double http, unnecessary
  def getAudio(word: String): Future[ServiceResponse] = {
    val ValidWordRequest = "[a-zA-Z\\s]{0,20}".r
    word match {
      case ValidWordRequest() =>
        if (Files.exists(Paths.get("src/main/resources/$word.wav"))) {
          val response: Future[Option[AudioResponse]] = Http()
            .singleRequest(HttpRequest(
              HttpMethods.POST,
              Uri(s"https://iam.bluemix.net/identity/token" +
                s"?grant_type=urn:ibm:params:oauth:grant-type:apikey" +
                s"&apikey=$ibmKey")
            ).withHeaders(
              RawHeader("Content-Type", "application/x-www-form-urlencoded"),
              RawHeader("Accept", "application/json"),
              RawHeader("Authorization", "Basic Yng6Yng=")
            ))
            .flatMap(Unmarshal(_).to[Option[AudioResponse]])
          response.flatMap {
            case None =>
              Future.successful(ServiceResponse(false, "Ошибка на стороне IBM!"))
            case Some(response) =>
              val data = AudioRequest(word)
              val answer: Future[HttpResponse] = Http()
                .singleRequest(HttpRequest(
                  HttpMethods.POST,
                  Uri(s"https://gateway-lon.watsonplatform.net/text-to-speech/api/v1/synthesize" +
                    s"?apikey=$ibmKey"),
                  entity = HttpEntity(ContentTypes.`application/json`, write(data))
                ).withHeaders(
                  RawHeader("Content-Type", "application/json"),
                  RawHeader("Accept", "audio/wav"),
                  RawHeader("Authorization", s"Bearer ${response.access_token}")
                ))
                .flatMap(Unmarshal(_).to[HttpResponse])

              answer.map { response =>
                response.entity.dataBytes
                  .runWith(FileIO.toPath(
                    new File(s"src/main/resources/$word.wav").toPath))
              }
              Future.successful(ServiceResponse(true, "Успешно"))
          }
        }
        else
          Future.successful(ServiceResponse(true, "Уже существует!"))
      case _ =>
        Future.successful(ServiceResponse(false, "Неверный запрос!"))
    }
  }

}
