package com.fablerq.dd.models

import org.mongodb.scala.bson.ObjectId

case class Quiz(
      _id: ObjectId,
      title: String,
      description: String,
    )

case class QuizParams(
      title: Option[String] = None,
      description: Option[String] = None,
    )