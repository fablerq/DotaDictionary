package com.fablerq.dd.configs

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, MediaTypes}
import com.fablerq.dd.services.WordService.{WordData, WordServiceResponse, WordsData}
import spray.json.{DefaultJsonProtocol}

object JsonSupport extends SprayJsonSupport {
  import DefaultJsonProtocol._

  //json support for Word
  implicit val wordServiceResponseFormat = jsonFormat2(WordServiceResponse)
  implicit val wordDataFormat = jsonFormat3(WordData)
  implicit val wordsDataFormat = jsonFormat1(WordsData)



  implicit val mapMarshaller: ToEntityMarshaller[Map[String, Any]] = Marshaller.opaque { map =>
    HttpEntity(ContentType(MediaTypes.`application/json`), map.toString)
  }
}
