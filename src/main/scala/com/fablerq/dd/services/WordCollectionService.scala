package com.fablerq.dd.services

import com.fablerq.dd.models.{ ServiceResponse, WordCollection, WordCollectionParams }
import com.fablerq.dd.repositories.WordCollectionRepository
import org.bson.types.ObjectId
import org.mongodb.scala.bson.ObjectId

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait WordCollectionService {
  def getAllWordCollections: Future[Either[ServiceResponse, Seq[WordCollection]]]
  def getWordCollection(id: String): Future[Either[ServiceResponse, WordCollection]]
  def addWordCollection(params: WordCollectionParams): Future[ServiceResponse]
  def updateWordCollectionDescription(params: WordCollectionParams): Future[ServiceResponse]
  def deleteWordCollection(id: String): Future[ServiceResponse]
  def deleteWordFromWordCollection(collection: String, id: String): Future[ServiceResponse]
  def addWordToWordCollection(collectionId: String, wordId: String): Future[ServiceResponse]
}

class WordCollectionServiceImpl(wordCollectionRepository: WordCollectionRepository)
    extends WordCollectionService {

  def getAllWordCollections: Future[Either[ServiceResponse, Seq[WordCollection]]] = {
    wordCollectionRepository.getAll.map {
      case x: Seq[WordCollection] if x.nonEmpty => Right(x)
      case _ =>
        Left(ServiceResponse(false, "База данных коллекций слов пуста"))
    }
  }

  def getWordCollection(id: String): Future[Either[ServiceResponse, WordCollection]] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      wordCollectionRepository.getById(objectId).map {
        case wordCollection: WordCollection =>
          Right(wordCollection)
        case _ =>
          Left(ServiceResponse(false, "Коллекция слов не найдена!"))
      }
    } else Future(Left(ServiceResponse(false, "Неверный запрос!")))
  }

  def addWordCollection(params: WordCollectionParams): Future[ServiceResponse] = params match {
    case WordCollectionParams(Some(title), Some(description), None) =>
      wordCollectionRepository.getByTitle(title).map {
        case _: WordCollection =>
          ServiceResponse(false, s"Коллекция слов ${params.title} уже создана")
        case _ =>
          wordCollectionRepository.addWordCollection(WordCollection.apply(
            new ObjectId(),
            title,
            description,
            List()
          ))
          ServiceResponse(true, "Коллекция слов успешно добавлена")
      }
    case _ =>
      Future(ServiceResponse(false, s"Коллекцию слов ${params.title} " +
        s"не удалось добавить. Неверный запрос"))
  }

  def updateWordCollectionDescription(params: WordCollectionParams): Future[ServiceResponse] =
    params match {
      case WordCollectionParams(Some(title), Some(translate), None) =>
        wordCollectionRepository.getByTitle(title).map {
          case wordCollection: WordCollection =>
            wordCollectionRepository.updateWordCollectionDescription(
              wordCollection._id,
              params.description.get
            )
            ServiceResponse(true, "Описание коллекции слов успешно обновлено")
          case _ =>
            ServiceResponse(false, s"Коллекция слов $title не найдена")
        }
      case _ =>
        Future(ServiceResponse(
          false, s"Коллекцию ${params.title} не удалось обновить. Неверный запрос"
        ))
    }

  def deleteWordCollection(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      wordCollectionRepository.getById(objectId).map {
        case wordCollection: WordCollection =>
          wordCollectionRepository.deleteWordCollection(wordCollection._id)
          ServiceResponse(true, "Коллекция успешно удалена")
        case _ =>
          ServiceResponse(false, "Не удалось удалить коллекцию")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

  def deleteWordFromWordCollection(collection: String, word: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(collection)) {
      val objectId = new ObjectId(collection)
      wordCollectionRepository.getById(objectId).map {
        case collection: WordCollection =>
          if (collection.words.contains(word)) {
            wordCollectionRepository.deleteWordToWordCollection(objectId, word)
            ServiceResponse(
              false, s"Слово $word успешно удалено в коллекции $collection"
            )
          } else ServiceResponse(false, s"Слово в коллекции отсутствует")
        case _ =>
          ServiceResponse(false, s"Коллекция $collection не существует")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

  //TO-DO, check if word exists in simple words
  def addWordToWordCollection(collectionId: String, word: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(collectionId)) {
      val objectId = new ObjectId(collectionId)
      wordCollectionRepository.getById(objectId).map {
        case collection: WordCollection =>
          if (collection.words.contains(word)) {
            ServiceResponse(false, s"Слово в коллекции уже присутствует")
          } else
            wordCollectionRepository.addWordToWordCollection(objectId, word)
          ServiceResponse(
            false,
            s"Слово $word успешно добавлено в коллекцию $collectionId"
          )
        case _ =>
          ServiceResponse(false, s"Не удалось добавить слово. Коллекция $collectionId не найдена")
      }
    } else {
      Future(ServiceResponse(false, "Неверный запрос!"))
    }
  }

}
