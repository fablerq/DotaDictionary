package com.fablerq.dd.repositories

import com.fablerq.dd.models.{Question, Quiz}
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{addToSet, pullByFilter}
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import org.mongodb.scala.{Completed, MongoCollection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class QuizRepository(quizCollection: MongoCollection[Quiz]) {

  def count = quizCollection.count().toFuture()

  def getAll =
    quizCollection.find().toFuture()

  def getByTitle(title: String): Future[Quiz] =
    quizCollection.find(equal("title", title))
      .first()
      .toFuture()

  def getById(id: ObjectId): Future[Quiz] =
    quizCollection
      .find(Document("_id" -> id))
      .first().toFuture()

  def addQuiz(quiz: Quiz): Future[Completed] =
    quizCollection.insertOne(quiz).toFuture()

  def updateQuizScore(id: ObjectId, newScore: Int): Future[UpdateResult] = {
    quizCollection.updateOne(
      Document("_id" -> id),
      Document("$set" -> Document("score" -> newScore))
    ).toFuture()
  }

  def updateQuizDoneSteps(id: ObjectId, doneSteps: Int): Future[UpdateResult] = {
    quizCollection.updateOne(
      Document("_id" -> id),
      Document("$set" -> Document("doneSteps" -> doneSteps))
    ).toFuture()
  }

  def updateQuizStatus(id: ObjectId): Future[UpdateResult] = {
    quizCollection.updateOne(
      Document("_id" -> id),
      Document("$set" -> Document("isClose" -> true))
    ).toFuture()
  }

  def deleteQuiz(id: ObjectId): Future[DeleteResult] =
    quizCollection.deleteOne(Document("_id" -> id)).toFuture()

  def addQuestion(id: ObjectId, question: Question) =
    quizCollection.updateOne(
      Document("_id" -> id),
      addToSet("questions", question)
    ).toFuture()

  def deleteQuestion(id: ObjectId, step: Int) =
    quizCollection.updateOne(
      Document("_id" -> id),
      pullByFilter(Document("questions" -> Document("step" -> step)))
    ).toFuture()

}
