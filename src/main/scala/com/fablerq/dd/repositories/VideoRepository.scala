package com.fablerq.dd.repositories

import com.fablerq.dd.configs.Mongo
import com.fablerq.dd.models.{ Stat, Video }
import org.mongodb.scala.{ Completed, MongoCollection }
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{ addToSet, pullByFilter }
import org.mongodb.scala.result.{ DeleteResult, UpdateResult }

import scala.concurrent.Future

class VideoRepository(videoCollection: MongoCollection[Video]) {

  def count = videoCollection.count().toFuture()

  def getAll =
    videoCollection.find().toFuture()

  def getByTitle(title: String): Future[Video] =
    videoCollection.find(equal("title", title))
      .first()
      .toFuture()

  def getById(id: ObjectId): Future[Video] =
    videoCollection
      .find(Document("_id" -> id))
      .first().toFuture()

  def getByLink(id: String): Future[Video] =
    videoCollection
      .find(Document("link" -> id))
      .first()
      .toFuture()

  def addVideo(video: Video): Future[Completed] =
    videoCollection.insertOne(video).toFuture()

  def updateVideoDescription(id: ObjectId, description: String): Future[UpdateResult] = {
    videoCollection.updateOne(
      Document("_id" -> id),
      Document("$set" -> Document("description" -> description))
    ).toFuture()
  }

  def deleteVideo(id: ObjectId): Future[DeleteResult] =
    videoCollection.deleteOne(Document("_id" -> id)).toFuture()

  def addStatToVideo(id: ObjectId, stat: Stat): Future[UpdateResult] = {
    videoCollection.updateOne(
      Document("_id" -> id),
      addToSet("stats", stat)
    ).toFuture()
  }

  def deleteStatFromVideo(id: ObjectId, title: String): Future[UpdateResult] = {
    videoCollection.updateOne(
      Document("_id" -> id),
      pullByFilter(Document("stats" -> Document("collectionId" -> title)))
    ).toFuture()
  }
}
