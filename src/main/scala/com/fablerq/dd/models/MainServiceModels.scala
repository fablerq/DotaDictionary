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

