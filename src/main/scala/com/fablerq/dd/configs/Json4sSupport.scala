package com.fablerq.dd.configs

import java.lang.reflect.InvocationTargetException
import java.util.UUID

import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import akka.stream.Materializer
import akka.util.ByteString
import org.json4s.JsonAST.{ JString, JValue }
import org.json4s.reflect.TypeInfo
import org.json4s.{ DefaultFormats, Formats, MappingException, Serialization, Serializer, jackson }
import org.mongodb.scala.bson.ObjectId

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext

class UUIDSerializer extends Serializer[UUID] {
  private[this] val UUIDClass = classOf[UUID]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), UUID] = {
    case (TypeInfo(UUIDClass, _), json) => json match {
      case JString(value) => UUID.fromString(value)
      case x => throw new MappingException(s"Can't convert $x to UUID")
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: UUID => JString(x.toString)
    case y: ObjectId => JString(y.toString)
  }
}

// https://github.com/hseeberger/akka-http-json/blob/master/akka-http-json4s/src/main/scala/de/heikoseeberger/akkahttpjson4s/Json4sSupport.scala

/**
 * Automatic to and from JSON marshalling/unmarshalling using an in-scope *Json4s* protocol.
 *
 * Pretty printing is enabled if an implicit [[Json4sSupport.ShouldWritePretty.True]] is in scope.
 */
object Json4sSupport extends Json4sSupport {

  sealed abstract class ShouldWritePretty

  final object ShouldWritePretty {
    final object True extends ShouldWritePretty
    final object False extends ShouldWritePretty
  }

}

/**
 * Automatic to and from JSON marshalling/unmarshalling using an in-scope *Json4s* protocol.
 *
 * Pretty printing is enabled if an implicit [[Json4sSupport.ShouldWritePretty.True]] is in scope.
 */
trait Json4sSupport {
  import Json4sSupport._

  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats ++ Seq(new UUIDSerializer())

  def unmarshallerContentTypes: Seq[ContentTypeRange] =
    mediaTypes.map(ContentTypeRange.apply)

  def mediaTypes: Seq[MediaType.WithFixedCharset] =
    List(`application/json`)

  private val jsonStringUnmarshaller =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(unmarshallerContentTypes: _*)
      .mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset) => data.decodeString(charset.nioCharset.name)
      }

  private val jsonStringMarshaller =
    Marshaller.oneOf(mediaTypes: _*)(Marshaller.stringMarshaller)

  /**
   * HTTP entity => `A`
   *
   * @tparam A type to decode
   * @return unmarshaller for `A`
   */

  implicit def unmarshaller[A: Manifest](implicit
    serialization: Serialization,
    formats: Formats): FromEntityUnmarshaller[A] =
    jsonStringUnmarshaller
      .map(s => serialization.read(s))
      .recover(throwCause)

  /**
   * `A` => HTTP entity
   *
   * @tparam A type to encode, must be upper bounded by `AnyRef`
   * @return marshaller for any `A` value
   */
  implicit def marshaller[A <: AnyRef](implicit
    serialization: Serialization,
    formats: Formats,
    shouldWritePretty: ShouldWritePretty = ShouldWritePretty.False): ToEntityMarshaller[A] =
    shouldWritePretty match {
      case ShouldWritePretty.False =>
        jsonStringMarshaller.compose(serialization.write[A])
      case ShouldWritePretty.True =>
        jsonStringMarshaller.compose(serialization.writePretty[A])
    }

  private def throwCause[A](
    ec: ExecutionContext
  )(mat: Materializer): PartialFunction[Throwable, A] = {
    case MappingException(_, e: InvocationTargetException) => throw e.getCause
  }
}
