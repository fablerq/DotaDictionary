package com.fablerq.dd.configs

import com.fablerq.dd.models._
import com.mongodb.MongoCredential._
import com.mongodb.{ MongoCredential, ServerAddress }
import com.mongodb.connection.{ ClusterSettings, ConnectionPoolSettings }
import com.typesafe.config.ConfigFactory
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }
import org.mongodb.scala.{ MongoClient, MongoClientSettings, MongoCollection, MongoDatabase, ServerAddress }
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

import scala.collection.JavaConverters._

object Mongo {
  lazy val config = ConfigFactory.load()

  lazy val connectionPoolSettings: ConnectionPoolSettings =
    ConnectionPoolSettings
      .builder()
      .maxWaitQueueSize(3000)
      .build()

  lazy val clusterSettings: ClusterSettings =
    ClusterSettings
      .builder()
      .maxWaitQueueSize(2000)
      .hosts(List(
        new ServerAddress(config.getString("mongo.host"))
      ).asJava)
      .build()

  val host: String = config.getString("mongo.host")
  val port: Int = config.getInt("mongo.port")
  val dbName: String = config.getString("mongo.database")
  val user: String = config.getString("mongo.user")
  val password: String = config.getString("mongo.password")

  val credential: MongoCredential =
    createCredential(user, dbName, password.toCharArray)

  lazy val settings: MongoClientSettings =
    MongoClientSettings
      .builder()
      .clusterSettings(clusterSettings)
      .credentialList(List(credential).asJava)
      .connectionPoolSettings(connectionPoolSettings)
      .build()

  lazy val mongoClient: MongoClient =
    MongoClient(settings)

  private val customCodecs = fromProviders(
    classOf[Word],
    classOf[WordCollection],
    classOf[Quiz],
    classOf[Dwi],
    classOf[Stat],
    classOf[WordStat],
    classOf[Article],
    classOf[Video]
  )

  lazy val codecRegistry = fromRegistries(customCodecs, DEFAULT_CODEC_REGISTRY)
  lazy val database: MongoDatabase =
    mongoClient
      .getDatabase(config.getString("mongo.database"))
      .withCodecRegistry(codecRegistry)

  //word collection
  val wordCollection: MongoCollection[Word] =
    database.getCollection("words")

  //wordCollection collection
  val wordCollectionCollection: MongoCollection[WordCollection] =
    database.getCollection("wordcollections")

  //article collection
  val articleCollection: MongoCollection[Article] =
    database.getCollection("articles")

  //video collection
  val videoCollection: MongoCollection[Video] =
    database.getCollection("videos")

  //quiz collection
  val quizCollection: MongoCollection[Quiz] =
    database.getCollection("quizzes")

  //dwi collection
  val dwiCollection: MongoCollection[Dwi] =
    database.getCollection("dwis")
}
