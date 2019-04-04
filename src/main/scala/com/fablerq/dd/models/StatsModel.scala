package com.fablerq.dd.models

case class Stat(
       collectionId: String,
       percent: Int,
  )

case class WordStat(
       word: String,
       count: Int
  )
