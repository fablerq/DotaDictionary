package com.fablerq.dd.models

import org.mongodb.scala.bson.ObjectId

object Word {

  def apply(
       title: String,
       translate: String,
       quantity: Int,
  ): Word = {
    new Word(new ObjectId(), title, translate, quantity)
  }

  def apply(
       _id: ObjectId,
       title: String,
       translate: String,
       quantity: Int,
  ): Word = new Word(_id, title, translate, quantity)

}

case class Word(
     _id: ObjectId,
     title: String,
     translate: String,
     quantity: Int
) {

}

//Todo: добавить поддержку валидации по сервису
case class WordResource(title: String, translate: String, quantity: Int) {
  require(title != null, "Название слова обязательно")
  require(title.nonEmpty, "Слово не может быть пустым")
}