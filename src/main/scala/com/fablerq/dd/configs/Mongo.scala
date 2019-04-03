package com.fablerq.dd.configs

import com.fablerq.dd.models._
import com.typesafe.config.ConfigFactory
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
import org.mongodb.scala.{ MongoClient, MongoCollection, MongoDatabase }
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

object Mongo {
  lazy val config = ConfigFactory.load()
  lazy val mongoClient: MongoClient = MongoClient(config.getString("mongo.uri"))

  private val customCodecs = fromProviders(
    classOf[Word],
    classOf[WordCollection],
    classOf[Quiz],
    classOf[Dwi],
    classOf[Stat],
    classOf[Article],
    classOf[Video]
  )

  lazy val codecRegistry = fromRegistries(customCodecs, DEFAULT_CODEC_REGISTRY)
  lazy val database: MongoDatabase = mongoClient.getDatabase(config.getString("mongo.database")).withCodecRegistry(codecRegistry)

  //word collection
  val wordCollection: MongoCollection[Word] = database.getCollection("words")
  def getWordCollection: MongoCollection[Word] = wordCollection

  //wordCollection collection
  val wordCollectionCollection: MongoCollection[WordCollection] = database.getCollection("wordcollections")
  def getWordCollectionCollection: MongoCollection[WordCollection] = wordCollectionCollection

  //article collection
  val articleCollection: MongoCollection[Article] = database.getCollection("articles")
  def getArticleCollection: MongoCollection[Article] = articleCollection

  //video collection
  val videoCollection: MongoCollection[Video] = database.getCollection("videos")
  def getVideoCollection: MongoCollection[Video] = videoCollection

  //quiz collection
  val quizCollection: MongoCollection[Quiz] = database.getCollection("quizzes")
  def getQuizCollection: MongoCollection[Quiz] = quizCollection

  //dwi collection
  val dwiCollection: MongoCollection[Dwi] = database.getCollection("dwis")
  def getDwiCollection: MongoCollection[Dwi] = dwiCollection
}
