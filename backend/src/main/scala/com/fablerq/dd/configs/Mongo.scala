package com.fablerq.dd.configs

import com.fablerq.dd.models.Word
import com.typesafe.config.ConfigFactory
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._



object Mongo {
  lazy val config = ConfigFactory.load()
  lazy val mongoClient: MongoClient = MongoClient(config.getString("mongo.uri"))

  private val customCodecs = fromProviders(classOf[Word])

  lazy val codecRegistry = fromRegistries(customCodecs, DEFAULT_CODEC_REGISTRY)
  lazy val database: MongoDatabase = mongoClient.getDatabase(config.getString("mongo.database")).withCodecRegistry(codecRegistry)

  //word collection
  val WORD_COLLECTION = "words"
  val wordCollection: MongoCollection[Word] = database.getCollection(WORD_COLLECTION)
  def getWordCollection = wordCollection

}
