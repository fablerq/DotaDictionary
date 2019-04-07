package com.fablerq.dd.repositories

import com.fablerq.dd.models.{ Article, Stat, WordStat }
import org.mongodb.scala.{ Completed, MongoCollection }
import org.mongodb.scala.bson.{ BsonDocument, ObjectId }
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.result.{ DeleteResult, UpdateResult }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArticleRepository(articleCollection: MongoCollection[Article]) {

  //val articleCollection = Mongo.getArticleCollection

  def count = articleCollection.count().toFuture()

  def getAll =
    //articleCollection.find(descending("words")).toFuture()
    articleCollection.find().toFuture()

  def getByTitle(title: String): Future[Article] =
    articleCollection.find(equal("title", title))
      .first()
      .toFuture()

  def getById(id: ObjectId): Future[Article] =
    articleCollection
      .find(Document("_id" -> id))
      .first().toFuture()

  def getByLink(id: String): Future[Article] =
    articleCollection
      .find(Document("link" -> id))
      .first().toFuture()

  def addArticle(article: Article): Future[Completed] =
    articleCollection.insertOne(article).toFuture()

  def updateArticleLink(id: ObjectId, link: String): Future[UpdateResult] = {
    articleCollection.updateOne(
      Document("_id" -> id),
      Document("$set" -> Document("link" -> link))
    ).toFuture()
  }

  def deleteArticle(id: ObjectId): Future[DeleteResult] =
    articleCollection.deleteOne(Document("_id" -> id)).toFuture()

  def addSomeStatsWordToArticle(id: ObjectId, list: List[WordStat]): Future[Article] = {
    articleCollection.updateOne(
      Document("_id" -> id),
      addEachToSet("words", list.map(r => r): _*)
    ).toFuture()
      .flatMap { _ =>
        getById(id)
          .map(x => x)
      }
  }

  def addStatWordToArticle(id: ObjectId, wordStat: WordStat): Future[UpdateResult] = {
    articleCollection.updateOne(
      Document("_id" -> id),
      addToSet("words", wordStat)
    ).toFuture()
  }

  def deleteStatWordFromArticle(id: ObjectId, word: String): Future[UpdateResult] = {
    articleCollection.updateOne(
      Document("_id" -> id),
      pullByFilter(Document("words" -> Document("word" -> word)))
    ).toFuture()
  }

  def addStatToArticle(id: ObjectId, stat: Stat): Future[UpdateResult] = {
    articleCollection.updateOne(
      Document("_id" -> id),
      addToSet("stats", stat)
    ).toFuture()
  }

  def deleteStatFromArticle(id: ObjectId, title: String): Future[UpdateResult] = {
    articleCollection.updateOne(
      Document("_id" -> id),
      pullByFilter(Document("stats" -> Document("collectionId" -> title)))
    ).toFuture()
  }

}
