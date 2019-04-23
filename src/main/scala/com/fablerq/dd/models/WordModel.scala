package com.fablerq.dd.models

import org.mongodb.scala.bson.ObjectId

case class Word(
  _id: ObjectId,
  title: String,
  translate: String,
  frequency: Long,
  quantity: Long
)

case class WordParams(
  title: Option[String] = None,
  translate: Option[String] = None,
  frequency: Option[Long] = None,
  quantity: Option[Long] = None
)