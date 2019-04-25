package com.fablerq.dd.models

import org.mongodb.scala.bson.ObjectId

case class Quiz(
      _id: ObjectId,
      title: String,
      collection: String,
      level: String,
      date: String,
      questions: List[Question],
      //not good practise to send true answers in real apps
      answers: List[String],
      quizType: Int,
      doneSteps: Int,
      totalSteps: Int,
      score: Int,
      isClose: Boolean
    )

case class QuizParams(
      title: Option[String] = None,
      collection: Option[String] = None,
      level: Option[String] = None,
      date: Option[String] = None,
      questions: Option[List[Question]] = None,
      answers: Option[List[String]] = None,
      quizType: Option[Int] = None,
      doneSteps: Option[Int] = None,
      totalSteps: Option[Int] = None,
      score:  Option[Int] = None,
      isClose: Option[Boolean] = None
    )

case class Question(
     title: String,
     questionType: Int,
     responseOptions: Option[List[String]] = None,
     step: Int,
     userAnswer: Option[String] = None,
     audioTitle: Option[String] = None
     )
