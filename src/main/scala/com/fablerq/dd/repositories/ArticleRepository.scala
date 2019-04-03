package com.fablerq.dd.repositories

import com.fablerq.dd.configs.Mongo
import com.fablerq.dd.models.{ Article, Stat }
import org.mongodb.scala.Completed
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{ addToSet, pullByFilter }
import org.mongodb.scala.result.{ DeleteResult, UpdateResult }

import scala.concurrent.Future

class ArticleRepository {

  val articleCollection = Mongo.getArticleCollection

  def count = articleCollection.count().toFuture()

  def getAll =
    articleCollection.find().toFuture()

  def getByTitle(title: String): Future[Article] =
    articleCollection.find(equal("title", title)).first().toFuture()

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

  def updateArticleBody(id: ObjectId, body: String): Future[UpdateResult] = {
    articleCollection.updateOne(
      Document("_id" -> id),
      Document("$set" -> Document("body" -> body))
    ).toFuture()
  }

  def deleteArticle(id: ObjectId): Future[DeleteResult] =
    articleCollection.deleteOne(Document("_id" -> id)).toFuture()

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
