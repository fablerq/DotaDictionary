package com.fablerq.dd.repositories

import com.fablerq.dd.configs.Mongo
import com.fablerq.dd.models.Word
import org.bson.types.ObjectId
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.model.Filters._

import scala.concurrent.Future

class WordRepository {

  val wordCollection = Mongo.getWordCollection

  def getByTitle(title: String): Future[Word] =
    wordCollection.find(equal("title", title)).first().toFuture()

  def getById(id: ObjectId) =
    wordCollection
      .find(Document("_id" -> id))
      .first().toFuture()

  def getAll =
    wordCollection.find().toFuture()

  def addWord(word: Word) =
    wordCollection.insertOne(word).toFuture()

  def deleteWord(id: ObjectId) =
    wordCollection.deleteOne(Document("_id" -> id)).toFuture()

  def updateQuantity(title: String, quantity: Int) = {
    val PlusOneToQuantity = quantity + 1
    wordCollection.updateOne(
      Document("title" -> title),
      Document("$set" -> Document("quantity" -> PlusOneToQuantity))
    ).toFuture()
  }

}
