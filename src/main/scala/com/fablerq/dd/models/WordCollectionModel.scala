package com.fablerq.dd.models

import org.mongodb.scala.bson.ObjectId

case class WordCollection(
  _id: ObjectId,
  title: String,
  description: String,
  words: List[String]
)

case class WordCollectionParams(
  title: Option[String] = None,
  description: Option[String] = None,
  words: Option[List[String]] = None
)