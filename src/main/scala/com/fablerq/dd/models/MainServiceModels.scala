package com.fablerq.dd.models

case class ArticleResponse(
  author: String,
  image: String,
  tags: List[String],
  article: String,
  videos: List[String],
  title: String,
  publishDate: String,
  feeds: List[String]
)

case class TranslateResponse(
  code: Int,
  lang: Option[String] = None,
  text: List[String]
)

case class AudioResponse(
        access_token: String,
        refresh_token: String,
        token_type: String,
        expires_in: Int,
        expiration: Int,
        scope: String
      )

sealed abstract class JValue
case class AudioRequest(
               text: String
               ) extends JValue

