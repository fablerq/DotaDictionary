package com.fablerq.dd.repositories

import com.fablerq.dd.configs.Mongo
import com.fablerq.dd.models.{ WordCollection, WordCollectionParams }
import org.mongodb.scala.Completed
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.result.{ DeleteResult, UpdateResult }

import scala.concurrent.Future

class WordCollectionRepository {

  val WordCollectionCollection = Mongo.getWordCollectionCollection

  def count = WordCollectionCollection.count().toFuture()

  def getAll =
    WordCollectionCollection.find().toFuture()

  def getByTitle(title: String): Future[WordCollection] =
    WordCollectionCollection.find(equal("title", title)).first().toFuture()

  def getById(id: ObjectId): Future[WordCollection] =
    WordCollectionCollection
      .find(Document("_id" -> id))
      .first().toFuture()

  def addWordCollection(wordCollection: WordCollection): Future[Completed] =
    WordCollectionCollection.insertOne(wordCollection).toFuture()

  def updateWordCollectionDescription(id: ObjectId, description: String): Future[UpdateResult] = {
    WordCollectionCollection.updateOne(
      Document("_id" -> id),
      Document("$set" -> Document("description" -> description))
    ).toFuture()
  }

  def deleteWordCollection(id: ObjectId): Future[DeleteResult] =
    WordCollectionCollection.deleteOne(Document("_id" -> id)).toFuture()

  def addWordToWordCollection(id: ObjectId, wordId: String): Future[UpdateResult] = {
    WordCollectionCollection.updateOne(
      Document("_id" -> id),
      addToSet("words", wordId)
    ).toFuture()
  }

  def deleteWordToWordCollection(id: ObjectId, wordId: String): Future[UpdateResult] = {
    WordCollectionCollection.updateOne(
      Document("_id" -> id),
      pullByFilter(Document("words" -> wordId))
    ).toFuture()
  }
}
