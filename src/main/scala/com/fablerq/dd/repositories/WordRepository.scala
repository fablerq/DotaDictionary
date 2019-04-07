package com.fablerq.dd.repositories

import com.fablerq.dd.configs.Mongo
import com.fablerq.dd.models.{ Word, WordParams }
import org.mongodb.scala.{ Completed, MongoCollection }
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates.{ combine, set }
import org.mongodb.scala.result.{ DeleteResult, UpdateResult }

import scala.concurrent.Future

class WordRepository(wordCollection: MongoCollection[Word]) {

  def count = wordCollection.count().toFuture()

  def getAll =
    wordCollection.find().toFuture()

  def getByTitle(title: String): Future[Word] =
    wordCollection.find(equal("title", title)).first().toFuture()

  def getById(id: ObjectId): Future[Word] =
    wordCollection
      .find(Document("_id" -> id))
      .first().toFuture()

  def addWord(word: Word): Future[Completed] =
    wordCollection.insertOne(word).toFuture()

  def updateWordTranslate(id: ObjectId, translate: String): Future[UpdateResult] = {
    wordCollection.updateOne(
      Document("_id" -> id),
      Document("$set" -> Document("translate" -> translate))
    ).toFuture()
  }

  def deleteWord(id: ObjectId): Future[DeleteResult] =
    wordCollection.deleteOne(Document("_id" -> id)).toFuture()

  def updateQuantity(id: ObjectId, quantity: Long): Future[UpdateResult] = {
    val PlusOneToQuantity = quantity + 1
    wordCollection.updateOne(
      Document("_id" -> id),
      Document("$set" -> Document("quantity" -> PlusOneToQuantity))
    ).toFuture()
  }

}
