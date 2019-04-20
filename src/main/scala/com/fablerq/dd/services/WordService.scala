package com.fablerq.dd.services

import com.fablerq.dd.models.{ ServiceResponse, Word, WordParams }
import com.fablerq.dd.repositories.WordRepository
import org.bson.types.ObjectId
import org.mongodb.scala.bson.ObjectId

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait WordService {
  def getAllWords: Future[Either[ServiceResponse, Seq[Word]]]
  def getWordsByPage(page: Int): Future[Either[ServiceResponse, Seq[Word]]]
  def getWord(id: String): Future[Either[ServiceResponse, Word]]
  def getWordByTitle(title: String): Future[Option[Word]]
  def getByIdDirectly(id: ObjectId): Future[Option[Word]]
  def addWord(params: WordParams): Future[ServiceResponse]
  def updateWordTranslate(params: WordParams): Future[ServiceResponse]
  def deleteWord(id: String): Future[ServiceResponse]
  def deleteWordByTitle(title: String): Future[ServiceResponse]
  def updateQuantity(id: String): Future[ServiceResponse]
}

class WordServiceImpl(wordRepository: WordRepository) extends WordService {

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

  def getWordByTitle(title: String): Future[Option[Word]] = {
    wordRepository.getByTitle(title).map { x =>
      Option(x)
    }
  }

  def getByIdDirectly(id: ObjectId): Future[Option[Word]] =
    wordRepository.getById(id).map { x =>
      Option(x)
    }

  def addWord(params: WordParams): Future[ServiceResponse] = params match {
    case WordParams(Some(title), Some(translate), None) =>
      getWordByTitle(title).flatMap {
        case Some(x) =>
          Future.successful(
            ServiceResponse(false, s"Слово ${params.title} уже создано")
          )
        case None =>
          wordRepository.addWord(Word.apply(
            new ObjectId(), title,
            translate, 0, 1
          )).map { x =>
            ServiceResponse(true, "Слово успешно добавлено")
          }
      }
    case _ =>
      Future.successful(
        ServiceResponse(false,
          s"Слово ${params.title} не удалось добавить.Неверный запрос"))
  }

  def updateWordTranslate(params: WordParams): Future[ServiceResponse] = params match {
    case WordParams(Some(title), Some(translate), None) =>
      getWordByTitle(title).flatMap {
        case Some(x) =>
          wordRepository.updateWordTranslate(x._id, translate).map { x=>
            ServiceResponse(true, "Перевод слова успешно изменен")
          }
        case None =>
          Future.successful(ServiceResponse(false, s"Слово $title не найдено"))
      }
    case _ =>
      Future.successful(
        ServiceResponse(false,
          s"Слово ${params.title} не удалось обновить. Неверный запрос"))
  }

  def deleteWord(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          wordRepository.deleteWord(x._id).map { x =>
            ServiceResponse(true, "Слово успешно удалено")
          }
        case None =>
          Future.successful(ServiceResponse(false, "Не удалось удалить слово"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

  def deleteWordByTitle(title: String): Future[ServiceResponse] = {
    getWordByTitle(title).flatMap {
      case Some(x) =>
        wordRepository.deleteWord(x._id).map { x =>
          ServiceResponse(true, "Слово успешно удалено")
        }
      case None =>
        Future.successful(ServiceResponse(false, "Слова не существует"))
    }
  }

  def updateQuantity(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          wordRepository.updateQuantity(x._id, x.quantity).map { x =>
            ServiceResponse(true, "Количество повторений у слова обновлено")
          }
        case None =>
          Future.successful(
            ServiceResponse(false,
              "Количество повторений у слова не удалось обновить"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

}
