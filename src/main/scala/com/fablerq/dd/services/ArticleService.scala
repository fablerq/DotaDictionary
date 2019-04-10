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
  def getArticleByTitle(title: String): Future[Either[ServiceResponse, Article]]
  def setArticleTitle(title: String): String
  def getIdByLink(link: String): Future[Article]
  def addArticleDirectly(article: Article): Future[Boolean]
  def addArticle(params: ArticleParams): Future[ServiceResponse]
  def updateArticleLink(params: ArticleParams): Future[ServiceResponse]
  def deleteArticle(id: String): Future[ServiceResponse]
  def deleteArticleByTitle(title: String): Future[ServiceResponse]
  //words for article
  def addStatWordToArticle(article: String, word: String): Future[ServiceResponse]
  def addSomeStatsWordToArticle(
    article: String,
    list: List[WordStat]
  ): Future[Either[ServiceResponse, Article]]
  def updateWordStatForArticle(article: String, word: String): Future[ServiceResponse]
  def deleteStatWordFromArticle(article: String, word: String): Future[ServiceResponse]
  //stats for article
  def addStatToArticle(
    article: String,
    collectionId: String,
    percent: Int
  ): Future[ServiceResponse]
  def updateStatToArticle(
    article: String,
    collectionId: String,
    partOfPercent: Int
  ): Future[ServiceResponse]
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
      articleRepository.getById(objectId).map {
        case article: Article => Right(article)
        case _ => Left(ServiceResponse(false, "Статья не найдена!"))
      }
    } else Future(Left(ServiceResponse(false, "Неверный запрос!")))
  }

  def getArticleByTitle(title: String): Future[Either[ServiceResponse, Article]] = {
    articleRepository.getByTitle(title).map {
      case article: Article => Right(article)
      case _ =>
        Left(ServiceResponse(false, "Статья не найдена!"))
    }
  }

  def setArticleTitle(title: String): String = {
    if (title == " ") "Unknown article"
    else title
  }

  def getIdByLink(link: String): Future[Article] =
    articleRepository.getByLink(link)

  def addArticleDirectly(article: Article): Future[Boolean] =
    articleRepository.addArticle(article).flatMap { _ =>
      Future(true)
    }

  def addArticle(params: ArticleParams): Future[ServiceResponse] = params match {
    case ArticleParams(None, Some(title), Some(link), None) =>
      articleRepository.getByLink(link).map {
        case _: Article =>
          ServiceResponse(
            false, s"Статья с этой ссылки ${params.link} уже добавлена"
          )
        case _ =>
          articleRepository.addArticle(Article.apply(
            new ObjectId(), title,
            List(), link, List()
          ))
          ServiceResponse(true, "Статья успешно добавлена")
      }
    case ArticleParams(Some(id), Some(title), Some(link), None) =>
      val objectId = new ObjectId(id)
      articleRepository.getByLink(link).map {
        case _: Article =>
          ServiceResponse(
            false, s"Статья с этой ссылки ${params.link} уже добавлена"
          )
        case _ =>
          articleRepository.addArticle(Article.apply(
            objectId, title,
            List(), link, List()
          ))
          ServiceResponse(true, "Статья успешно добавлена")
      }
    case _ =>
      Future(ServiceResponse(
        false, s"Статью ${params.title} не удалось добавить. Неверный запрос"
      ))
  }

  def updateArticleLink(params: ArticleParams): Future[ServiceResponse] = params match {
    case ArticleParams(None, Some(title), Some(link), None) =>
      articleRepository.getByTitle(title).map {
        case article: Article =>
          articleRepository.updateArticleLink(article._id, params.link.get)
          ServiceResponse(true, "Ссылка статьи успешно обновлена")
        case _ =>
          ServiceResponse(false, s"Статья $title не найдена")
      }
    case _ =>
      Future(ServiceResponse(
        false, s"Статья ${params.title} не обновилась. Неверный запрос"
      ))
  }

  def deleteArticle(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      articleRepository.getById(objectId).map {
        case article: Article =>
          articleRepository.deleteArticle(article._id)
          ServiceResponse(true, "Статья успешно удалена")
        case _ =>
          ServiceResponse(false, "Не удалось удалить статью")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

  def deleteArticleByTitle(title: String): Future[ServiceResponse] = {
    articleRepository.getByTitle(title).map {
      case article: Article =>
        articleRepository.deleteArticle(article._id)
        ServiceResponse(true, "Статья успешно удалена")
      case _ =>
        ServiceResponse(false, "Не удалось удалить статью")
    }
  }

  def addStatWordToArticle(article: String, word: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(article)) {
      val objectId = new ObjectId(article)
      articleRepository.getById(objectId).map {
        case article: Article =>
          if (article.words.exists(_.word == word)) {
            ServiceResponse(false, s"Данное слово в статье уже присутствует")
          } else {
            articleRepository.addStatWordToArticle(objectId, WordStat(word, 1))
            ServiceResponse(true, s"Слово в статью успешно добавлена")
          }
        case _ =>
          ServiceResponse(false, s"Статья $article не существует")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

  def addSomeStatsWordToArticle(
    article: String,
    list: List[WordStat]
  ): Future[Either[ServiceResponse, Article]] = {
    if (ObjectId.isValid(article)) {
      val objectId = new ObjectId(article)
      articleRepository.addSomeStatsWordToArticle(objectId, list).map {
        case article: Article => Right(article)
        case _ => Left(
          ServiceResponse(false, s"Не получилось добавить статистику")
        )
      }
    } else Future(Left(ServiceResponse(false, "Неверный запрос!")))
  }

  def updateWordStatForArticle(article: String, word: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(article)) {
      val objectId = new ObjectId(article)
      articleRepository.getById(objectId).map {
        case article: Article =>
          if (article.words.exists(_.word == word)) {
            articleRepository.deleteStatWordFromArticle(objectId, word)
            articleRepository.addStatWordToArticle(
              objectId,
              WordStat(word, article.words.find(_.word == word).get.count + 1)
            )
            ServiceResponse(true, "Количество повторений у слова обновлено")
          } else {
            ServiceResponse(false, s"Данное слово в статье не найдено")
          }
        case _ =>
          ServiceResponse(false, "Количество повторений у слова не удалось обновить")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

  def deleteStatWordFromArticle(article: String, word: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(article)) {
      val objectId = new ObjectId(article)
      articleRepository.getById(objectId).map {
        case article: Article =>
          if (article.words.exists(_.word == word)) {
            articleRepository.deleteStatWordFromArticle(objectId, word)
            ServiceResponse(true, s"Слово в статье успешно удалено")
          } else {
            ServiceResponse(false, s"Данное слово в статье отсутствует")
          }
        case _ =>
          ServiceResponse(false, s"Статья $article не существует")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

  def addStatToArticle(
    article: String,
    collectionId: String,
    percent: Int
  ): Future[ServiceResponse] = {
    if (ObjectId.isValid(article) & ObjectId.isValid(collectionId)) {
      val objectIdArticle = new ObjectId(article)
      articleRepository.getById(objectIdArticle).map {
        case article: Article =>
          if (article.stats.exists(_.collectionId == collectionId)) {
            ServiceResponse(false, s"Статистика в статье уже присутствует")
          } else {
            articleRepository.addStatToArticle(
              objectIdArticle,
              Stat(collectionId, percent)
            )
            ServiceResponse(true, s"Статистика успешно добавлена")
          }
        case _ =>
          ServiceResponse(false, s"Статья $article не существует")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

  def updateStatToArticle(
    article: String,
    collectionId: String,
    partOfPercent: Int
  ): Future[ServiceResponse] = {

    if (ObjectId.isValid(article) & ObjectId.isValid(collectionId)) {
      val objectIdArticle = new ObjectId(article)
      articleRepository.getById(objectIdArticle).map {
        case article: Article =>
          //if this article if exists
          if (article.stats.exists(_.collectionId == collectionId)) {
            articleRepository.deleteStatFromArticle(objectIdArticle, collectionId)
            //plus new part of percent to old percent value
            articleRepository.addStatToArticle(
              objectIdArticle,
              Stat(
                collectionId,
                article.stats
                  .find(_.collectionId == collectionId)
                  .get
                  .percent + partOfPercent
              )
            )
            ServiceResponse(true, s"Статистика успешно обновлена")
          } else {
            ServiceResponse(false, s"Статистика в статье не найдена")
          }
        case _ =>
          ServiceResponse(false, s"Статья $article не существует")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

  def deleteStatFromArticle(article: String, collectionId: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(article) & ObjectId.isValid(collectionId)) {
      val objectId = new ObjectId(article)
      articleRepository.getById(objectId).map {
        case article: Article =>
          if (article.stats.exists(_.collectionId == collectionId)) {
            articleRepository.deleteStatFromArticle(objectId, collectionId)
            ServiceResponse(false, s"Статистика успешна удалена")
          } else {
            ServiceResponse(false, s"Статистика в статье отсутствует")
          }
        case _ =>
          ServiceResponse(false, s"Статья $article не существует")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

}