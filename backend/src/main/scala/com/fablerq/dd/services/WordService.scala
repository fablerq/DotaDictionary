package com.fablerq.dd.services

import com.fablerq.dd.models.Word
import com.fablerq.dd.repositories.WordRepository
import com.fablerq.dd.services.WordService.{WordData, WordServiceResponse, WordsData}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent._
import ExecutionContext.Implicits.global

object WordService {
  case class WordServiceResponse(bool: Boolean, message: String)
  case class WordData(title: String, translate: String, quantity: Int)
  case class WordsData(words: Seq[WordData])
}

class WordService(wordRepository: WordRepository) {

  //check is word has already exists
  def existByTitle(title: String) = {
    Await.result(wordRepository.getByTitle(title), 1 seconds)
  }

  //get single word
  def getWord(title: String): Future[Either[WordServiceResponse, WordData]] = {
      wordRepository.getByTitle(title).map{
        case word: Word => Right(WordData(word.title, word.translate, word.quantity))
        case _ => Left(WordServiceResponse(false, "Слово не найдено!"))
      }
  }

  //get all words
  def getAllWords: Future[WordsData] = {
    var words: Seq[WordData] = Seq()
    val words2 = wordRepository.getAll.map { x => x.map { y =>
      words = words :+ WordData(y.title, y.translate, y.quantity)
    }}
    //ToDo: too unprofessional, will change this shit later
    Await.result(words2, 1 seconds)
    Future { new WordsData(words)}
  }

  //delete single word
  def deleteWord(title: String): Future[WordServiceResponse] = {
    wordRepository.getByTitle(title).map {
      case word: Word =>
        wordRepository.deleteWord(word._id)
        WordServiceResponse(true, "Слово успешно удалено")
      case _ => WordServiceResponse(false, "Слово не удалено")
    }
  }

  //update words'quantity
  def updateQuantity(title: String): Future[WordServiceResponse] = {
    wordRepository.getByTitle(title).map {
      case word: Word =>
        wordRepository.updateQuantity(title, word.quantity)
        WordServiceResponse(true, "Количество повторений у слова обновлено")
      case _ => WordServiceResponse(false, "Количество повторений у слова не удалось обновить")
    }
  }

  //add new single word
  def addWord(title: String, translate: String): WordServiceResponse = {
    if (existByTitle(title) != null)
      WordServiceResponse(false, s"Слово ${title} уже создано")
    else {
      wordRepository.addWord(Word.apply(title, translate, 1))
      WordServiceResponse(true, "Слово успешно добавлено")
    }
  }
}
