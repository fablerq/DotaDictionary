package com.fablerq.dd

import java.util.Date

import com.github.simplyscala.MongoEmbedDatabase
import org.mongodb.scala.MongoClient
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.fablerq.dd.configs.Json4sSupport
import org.scalatest._

import com.fablerq.dd.repositories.WordRepository._


class WordTests extends AsyncFlatSpec with Matchers with Json4sSupport {




}