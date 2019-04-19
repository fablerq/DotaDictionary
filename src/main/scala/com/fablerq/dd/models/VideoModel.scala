package com.fablerq.dd.models

import org.mongodb.scala.bson.ObjectId

case class Video(
  _id: ObjectId,
  title: String,
  description: String,
  words: List[WordStat],
  link: String,
  stats: List[Stat]
)

case class VideoParams(
  title: Option[String] = None,
  description: Option[String] = None,
  link: Option[String] = None,
  stats: Option[List[Stat]] = None
)
