package com.fablerq.dd.models

import org.mongodb.scala.bson.ObjectId

case class Article(
  _id: ObjectId,
  title: String,
  words: List[WordStat],
  link: String,
  stats: List[Stat]
)

case class ArticleParams(
  title: Option[String] = None,
  link: Option[String] = None,
  stats: Option[List[Stat]] = None
)
