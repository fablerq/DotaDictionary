package com.fablerq.dd.services

import com.fablerq.dd.models._
import com.fablerq.dd.repositories.ArticleRepository
import org.bson.types.ObjectId
import org.mongodb.scala.Completed
import org.mongodb.scala.bson.ObjectId

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ArticleService {
  //main functions for articles
  def getAllArticles: Future[Either[ServiceResponse, Seq[Article]]]
  def getArticle(id: String): Future[Either[ServiceResponse, Article]]
  def setArticleTitle(title: String): String
  def getByIdDirectly(id: ObjectId): Future[Option[Article]]
  def getIdByLink(link: String): Future[Option[Article]]
  def addArticleDirectly(article: Article): Future[Boolean]
  def addArticle(params: ArticleParams): Future[ServiceResponse]
  def updateArticleLink(params: ArticleParams): Future[ServiceResponse]
  def deleteArticle(id: String): Future[ServiceResponse]
  //words for article
  def addStatWordToArticle(article: String, word: String): Future[ServiceResponse]
  def updateWordStatForArticle(article: String, word: String): Future[ServiceResponse]
  def deleteStatWordFromArticle(article: String, word: String): Future[ServiceResponse]
  //stats for article
  def addStatToArticle(article: String, collectionId: String, percent: Int): Future[ServiceResponse]
  def deleteStatFromArticle(article: String, collectionId: String): Future[ServiceResponse]
}

class ArticleServiceImpl(articleRepository: ArticleRepository)
    extends ArticleService {

  def getAllArticles: Future[Either[ServiceResponse, Seq[Article]]] = {
    articleRepository.getAll.map {
      case x: Seq[Article] if x.nonEmpty => Right(x)
      case _ => Left(ServiceResponse(false, "База данных статей пуста"))
    }
  }

  def getArticle(id: String): Future[Either[ServiceResponse, Article]] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          val words: List[WordStat] = x.words.sortBy(-_.count)
          Future.successful(Right(Article(
            x._id,
            x.title,
            words,
            x.link,
            x.stats
          )))
        case None =>
          Future.successful(Left(ServiceResponse(false, "Статья не найдена!")))
      }
    } else Future.successful(Left(ServiceResponse(false, "Неверный запрос!")))
  }

  def setArticleTitle(title: String): String = {
    if (title == " ") "Unknown article"
    else title
  }

  def getByIdDirectly(id: ObjectId): Future[Option[Article]] =
    articleRepository.getById(id).map { x =>
      Option(x)
    }

  def getIdByLink(link: String): Future[Option[Article]] =
    articleRepository.getByLink(link).map { x =>
      Option(x)
    }

  def addArticleDirectly(article: Article): Future[Boolean] =
    articleRepository.addArticle(article).map(_ => true)

  def addArticle(params: ArticleParams): Future[ServiceResponse] = params match {
    case ArticleParams(None, Some(title), Some(link), None) =>
      getIdByLink(link).flatMap {
        case Some(x) =>
          Future.successful(
            ServiceResponse(
            false, s"Статья с этой ссылки ${params.link} уже добавлена"
          ))
        case None =>
          articleRepository.addArticle(Article.apply(
            new ObjectId(), title,
            List(), link, List()
          )).map { x=>
            ServiceResponse(true, "Статья успешно добавлена")
          }
      }
    case ArticleParams(Some(id), Some(title), Some(link), None) =>
      val objectId = new ObjectId(id)
      getIdByLink(link).flatMap {
        case None =>
          Future.successful(
            ServiceResponse(
              false, s"Статья с этой ссылки ${params.link} уже добавлена"))
        case Some(x) =>
          articleRepository.addArticle(Article.apply(
            objectId, title,
            List(), link, List()
          )).map { x=>
            ServiceResponse(true, "Статья успешно добавлена")
          }
      }
    case _ =>
      Future.successful(ServiceResponse(
        false, s"Статью ${params.title} не удалось добавить. Неверный запрос"
      ))
  }

  def updateArticleLink(params: ArticleParams): Future[ServiceResponse] = params match {
    case ArticleParams(None, Some(title), Some(link), None) =>
      getIdByLink(link).flatMap {
        case Some(x) =>
          articleRepository.updateArticleLink(x._id, params.link.get).map { x=>
            ServiceResponse(true, "Ссылка статьи успешно обновлена")
          }
        case None =>
          Future.successful(ServiceResponse(false, s"Статья $title не найдена"))
      }
    case _ =>
      Future.successful(ServiceResponse(
        false, s"Статья ${params.title} не обновилась. Неверный запрос"
      ))
  }

  def deleteArticle(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          articleRepository.deleteArticle(x._id).map { x =>
            ServiceResponse(true, "Статья успешно удалена")
          }
        case None =>
          Future.successful(
            ServiceResponse(false, "Не удалось удалить статью"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

  def addStatWordToArticle(article: String, word: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(article)) {
      val objectId = new ObjectId(article)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          if (x.words.exists(_.word == word))
            Future.successful(
              ServiceResponse(false, s"Данное слово в статье уже присутствует"))
          else {
            articleRepository.addStatWordToArticle(objectId, WordStat(word, 1))
              .map { x=>
              ServiceResponse(true, s"Слово в статью успешно добавлена")
            }
          }
        case _ =>
          Future.successful(
            ServiceResponse(false, s"Статья $article не существует"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

  def updateWordStatForArticle(article: String, word: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(article)) {
      val objectId = new ObjectId(article)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          if (x.words.exists(_.word == word)) {
            articleRepository.deleteStatWordFromArticle(objectId, word).flatMap { _ =>
              articleRepository.addStatWordToArticle(
                objectId,
                WordStat(word, x.words.find(_.word == word).get.count + 1)
              ).map { _ =>
                ServiceResponse(true, "Количество повторений у слова обновлено")
              }
            }
          }
          else
            Future.successful(
              ServiceResponse(false, s"Данное слово в статье не найдено"))
        case None =>
          Future.successful(
            ServiceResponse(false, "Количество повторений у слова не удалось обновить"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

  def deleteStatWordFromArticle(article: String, word: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(article)) {
      val objectId = new ObjectId(article)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          if (x.words.exists(_.word == word))
            articleRepository.deleteStatWordFromArticle(objectId, word).map { x=>
                ServiceResponse(true, s"Слово в статье успешно удалено")
            }
          else
            Future.successful(
              ServiceResponse(false, s"Данное слово в статье отсутствует"))
        case None =>
          Future.successful(ServiceResponse(false, s"Статья $article не существует"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

  def addStatToArticle(
    article: String,
    collectionId: String,
    percent: Int): Future[ServiceResponse] = {
    if (ObjectId.isValid(article) & ObjectId.isValid(collectionId)) {
      val objectIdArticle = new ObjectId(article)
      getByIdDirectly(objectIdArticle).flatMap {
        case Some(x) =>
          if (x.stats.exists(_.collectionId == collectionId))
            Future.successful(
              ServiceResponse(false, s"Статистика в статье уже присутствует"))
          else
            articleRepository.addStatToArticle(
              objectIdArticle,
              Stat(collectionId, percent)
            ).map { x =>
              ServiceResponse(true, s"Статистика успешно добавлена")
            }
        case None =>
          Future.successful(
            ServiceResponse(false, s"Статья $article не существует"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

  def deleteStatFromArticle(article: String, collectionId: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(article) & ObjectId.isValid(collectionId)) {
      val objectId = new ObjectId(article)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          if (x.stats.exists(_.collectionId == collectionId))
            articleRepository.deleteStatFromArticle(objectId, collectionId).map { x =>
                ServiceResponse(false, s"Статистика успешна удалена")
              }
          else
            Future.successful(
              ServiceResponse(false, s"Статистика в статье отсутствует"))
        case None =>
          Future.successful(
            ServiceResponse(false, s"Статья $article не существует"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

}