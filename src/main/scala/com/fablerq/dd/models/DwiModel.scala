package com.fablerq.dd.models

import org.mongodb.scala.bson.ObjectId

case class Dwi(
      _id: ObjectId,
      title: String,
      translate: String,
      frequency: String,
      quantity: String,
      sourceType: String,
    )

case class DwiParams(
      title: Option[String] = None,
      translate: Option[String] = None,
      frequency: Option[String] = None,
      quantity: Option[String] = None,
      sourceType: Option[String] = None,
    )
