package com.fablerq.dd.services

import com.fablerq.dd.models.{ ServiceResponse, Word, WordParams }
import com.fablerq.dd.repositories.WordRepository
import org.bson.types.ObjectId
import org.mongodb.scala.bson.ObjectId

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class WordService(wordRepository: WordRepository) {

  def getAllWords: Future[Either[ServiceResponse, Seq[Word]]] = {
    wordRepository.getAll.map {
      case x: Seq[Word] if x.nonEmpty => Right(x)
      case _ =>
        Left(ServiceResponse(false, "База данных слов пуста"))
    }
  }

  def getWordsByPage(page: Int): Future[Either[ServiceResponse, Seq[Word]]] = {
    wordRepository.getWordsByPage(page).map {
      case x: Seq[Word] if x.nonEmpty => Right(x)
      case _ =>
        Left(ServiceResponse(false, "База данных слов пуста"))
    }
  }

  def getWord(id: String): Future[Either[ServiceResponse, Word]] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      wordRepository.getById(objectId).map {
        case word: Word => Right(word)
        case _ =>
          Left(ServiceResponse(false, "Слово не найдено!"))
      }
    } else Future(Left(ServiceResponse(false, "Неверный запрос!")))
  }

  def getWordByTitle(title: String): Future[Word] = {
    wordRepository.getByTitle(title)
  }

  def addWord(params: WordParams): Future[ServiceResponse] = params match {
    case WordParams(Some(title), Some(translate), None) =>
      wordRepository.getByTitle(title).map {
        case _: Word =>
          ServiceResponse(false, s"Слово ${params.title} уже создано")
        case _ =>
          wordRepository.addWord(Word.apply(
            new ObjectId(), title,
            translate, 0, 1
          ))
          ServiceResponse(true, "Слово успешно добавлено")
      }
    case _ =>
      Future(ServiceResponse(false, s"Слово ${params.title} не удалось добавить." +
        s" Неверный запрос"))
  }

  def updateWordTranslate(params: WordParams): Future[ServiceResponse] = params match {
    case WordParams(Some(title), Some(translate), None) =>
      wordRepository.getByTitle(title).map {
        case word: Word =>
          wordRepository.updateWordTranslate(word._id, translate)
          ServiceResponse(true, "Перевод слова успешно изменен")
        case _ =>
          ServiceResponse(false, s"Слово $title не найдено")
      }
    case _ =>
      Future(ServiceResponse(false, s"Слово ${params.title} не удалось обновить." +
        s" Неверный запрос"))
  }

  def deleteWord(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      wordRepository.getById(objectId).map {
        case word: Word =>
          wordRepository.deleteWord(word._id)
          ServiceResponse(true, "Слово успешно удалено")
        case _ =>
          ServiceResponse(false, "Не удалось удалить слово")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

  def deleteWordByTitle(title: String): Future[ServiceResponse] = {
    wordRepository.getByTitle(title).map {
      case word: Word =>
        wordRepository.deleteWord(word._id)
        ServiceResponse(true, "Слово успешно удалено")
      case _ =>
        ServiceResponse(false, "Слова не существует")
    }
  }

  def updateQuantity(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      wordRepository.getById(objectId).map {
        case word: Word =>
          wordRepository.updateQuantity(word._id, word.quantity)
          ServiceResponse(true, "Количество повторений у слова обновлено")
        case _ =>
          ServiceResponse(false, "Количество повторений у слова не удалось обновить")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

}
