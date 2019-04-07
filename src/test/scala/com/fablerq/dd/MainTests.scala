package com.fablerq.dd

import akka.actor.ActorSystem
import org.mongodb.scala.{ MongoClient, MongoCollection, MongoDatabase }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.fablerq.dd.Server.httpService
import org.json4s.jackson.Serialization.write
import org.scalatest._
import com.fablerq.dd.configs.Mongo.{ codecRegistry, database }
import com.fablerq.dd.models._
import com.fablerq.dd.services.HttpService
import com.fablerq.dd.configs.Json4sSupport._
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.model.Updates._
import com.fablerq.dd.services._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{ Failure, Success }
import com.fablerq.dd.models.WordCollection
import com.fablerq.dd.repositories.WordCollectionRepository
import org.mongodb.scala.{ Completed, MongoCollection }
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.result.{ DeleteResult, UpdateResult }

import scala.concurrent.Future

class MainTests extends AsyncFlatSpec with Matchers {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  //Server setting
  //============================

  lazy val mongoClient: MongoClient =
    MongoClient("mongodb://heroku_4l5vj7zd:uoacb0q9ogb9o1de9t6289k6hu@ds233596.mlab.com:33596/heroku_4l5vj7zd")

  lazy val database: MongoDatabase =
    mongoClient.getDatabase("heroku_4l5vj7zd").withCodecRegistry(codecRegistry)

  val httpService = new HttpService(database)

  Http().bindAndHandle(httpService.routes, "0.0.0.0", 8080)

  database.drop().toFuture()

  //============================

  behavior of "Word"

  val wordParams = WordParams(Some("house"), Some("дом"), None)
  val word = Word(new ObjectId(), "house", "дом", 0, 1)

  it should "create new word" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.POST,
        Uri(s"http://localhost:8080/api/words"),
        entity = HttpEntity(ContentTypes.`application/json`, write(wordParams))
      ))
      .flatMap(Unmarshal(_).to[ServiceResponse])
      .map { x => x.message shouldBe "Слово успешно добавлено" }
  }

  it should "update first word" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.PATCH,
        Uri(s"http://localhost:8080/api/words"),
        entity = HttpEntity(ContentTypes.`application/json`, write(wordParams))
      ))
      .flatMap(Unmarshal(_).to[ServiceResponse])
      .map { x => x.message shouldBe "Перевод слова успешно изменен" }
  }

  it should "get all words" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.GET,
        Uri(s"http://localhost:8080/api/words")
      ))
      .flatMap(Unmarshal(_).to[List[Word]])
      .map { x => x.head.title shouldBe word.title }
  }

  it should "get first word" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.POST,
        Uri(s"http://localhost:8080/api/words?id=${wordParams.title.get}")
      ))
      .flatMap(Unmarshal(_).to[Word])
      .map { x => x.title shouldBe word.title }
  }

  it should "delete first word" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.DELETE,
        Uri(s"http://localhost:8080/api/words?id=${wordParams.title.get}")
      ))
      .flatMap(Unmarshal(_).to[ServiceResponse])
      .map { x => x.message shouldBe "Слово успешно удалено" }
  }

  behavior of "WordCollection"

  val wordCollectionParams = WordCollectionParams(
    Some("coll1"),
    Some("супер коллекция")
  )

  val wordCollection = WordCollection(
    new ObjectId(),
    "coll1",
    "супер коллекция",
    List()
  )

  it should "create new word сollection" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.POST,
        Uri(s"http://localhost:8080/api/wordcollections"),
        entity = HttpEntity(ContentTypes.`application/json`, write(wordCollectionParams))
      ))
      .flatMap(Unmarshal(_).to[ServiceResponse])
      .map { x => x.message shouldBe "Коллекция слов успешно добавлена" }
  }

  it should "update first word collection" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.PATCH,
        Uri(s"http://localhost:8080/api/wordcollections"),
        entity = HttpEntity(ContentTypes.`application/json`, write(wordCollectionParams))
      ))
      .flatMap(Unmarshal(_).to[ServiceResponse])
      .map { x => x.message shouldBe "Описание коллекции слов успешно обновлено" }
  }

  it should "get all word collections" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.GET,
        Uri(s"http://localhost:8080/api/wordcollections")
      ))
      .flatMap(Unmarshal(_).to[List[WordCollection]])
      .map { x => x.head.title shouldBe wordCollection.title }
  }

  behavior of "Article"

  val articleParams = ArticleParams(
    Some("article1"),
    Some("www.")
  )

  val article = Article(
    new ObjectId(),
    "article1",
    List(),
    "www.",
    List()
  )

  it should "create new article" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.POST,
        Uri(s"http://localhost:8080/api/articles"),
        entity = HttpEntity(ContentTypes.`application/json`, write(articleParams))
      ))
      .flatMap(Unmarshal(_).to[ServiceResponse])
      .map { x => x.message shouldBe "Статья успешно добавлена" }
  }

  it should "update first article" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.PATCH,
        Uri(s"http://localhost:8080/api/articles"),
        entity = HttpEntity(ContentTypes.`application/json`, write(articleParams))
      ))
      .flatMap(Unmarshal(_).to[ServiceResponse])
      .map { x => x.message shouldBe "Ссылка статьи успешно обновлена" }
  }

  it should "get all articles" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.GET,
        Uri(s"http://localhost:8080/api/articles")
      ))
      .flatMap(Unmarshal(_).to[List[Article]])
      .map { x => x.head.title shouldBe article.title }
  }

  behavior of "Video"

  val videoParams = VideoParams(
    Some("video1"),
    Some("super video"),
    Some("www.")
  )

  val video = Video(
    new ObjectId(),
    "video1",
    "super video",
    "www.",
    List()
  )

  it should "create new video" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.POST,
        Uri(s"http://localhost:8080/api/videos"),
        entity = HttpEntity(ContentTypes.`application/json`, write(videoParams))
      ))
      .flatMap(Unmarshal(_).to[ServiceResponse])
      .map { x => x.message shouldBe "Видео успешно успешно добавлено" }
  }

  it should "update first video" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.PATCH,
        Uri(s"http://localhost:8080/api/videos"),
        entity = HttpEntity(ContentTypes.`application/json`, write(videoParams))
      ))
      .flatMap(Unmarshal(_).to[ServiceResponse])
      .map { x => x.message shouldBe "Описание видео успешно обновлено" }
  }

  it should "get all videos" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.GET,
        Uri(s"http://localhost:8080/api/videos")
      ))
      .flatMap(Unmarshal(_).to[List[Video]])
      .map { x => x.head.title shouldBe video.title }
  }

  behavior of "Main"

  it should "return word MainServiceResponse" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.POST,
        Uri(s"http://localhost:8080/api/main?request=book")
      ))
      .flatMap(Unmarshal(_).to[MainServiceResponse])
      .map { x => x.responseType shouldBe Some("word") }
  }

  it should "return article MainServiceResponse" in {
    Http()
      .singleRequest(HttpRequest(
        HttpMethods.POST,
        Uri(s"http://localhost:8080/api/main?request=" +
          s"https://www.dotainternational.com/15-things-dota-2-pro-do-but-you-dont/")
      ))
      .flatMap(Unmarshal(_).to[MainServiceResponse])
      .map { x => x.responseType shouldBe Some("article") }
  }
}