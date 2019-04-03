package com.fablerq.dd.services

import com.fablerq.dd.models._
import com.fablerq.dd.repositories.ArticleRepository
import org.bson.types.ObjectId
import org.mongodb.scala.bson.ObjectId

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ArticleService(articleRepository: ArticleRepository) {

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
    } else {
      Future(Left(ServiceResponse(false, "Неверный запрос!")))
    }
  }

  def addArticle(params: ArticleParams): Future[ServiceResponse] = params match {
    case ArticleParams(Some(title), Some(body), Some(link), None) =>
      articleRepository.getByLink(link).map {
        case _: Article =>
          ServiceResponse(false, s"Статья с этой ссылки ${params.link} уже добавлена")
        case _ =>
          articleRepository.addArticle(Article.apply(
            new ObjectId(), title,
            body, link, List()
          ))
          ServiceResponse(true, "Статья успешно добавлена")
      }
    case _ => Future(ServiceResponse(false, s"Статью ${params.title} не удалось добавить. Неверный запрос"))
  }

  def updateArticleBody(params: ArticleParams): Future[ServiceResponse] = params match {
    case ArticleParams(Some(title), Some(body), None, None) =>
      articleRepository.getByTitle(title).map {
        case article: Article =>
          articleRepository.updateArticleBody(article._id, params.body.get)
          ServiceResponse(true, "Заглавие статьи успешно обновлено")
        case _ => ServiceResponse(false, s"Статья $title не найдена")
      }
    case _ => Future(ServiceResponse(false, s"Статья ${params.title} не обновилась. Неверный запрос"))
  }

  def deleteArticle(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      articleRepository.getById(objectId).map {
        case article: Article =>
          articleRepository.deleteArticle(article._id)
          ServiceResponse(true, "Статья успешно удалена")
        case _ => ServiceResponse(false, "Не удалось удалить статью")
      }
    } else {
      Future(ServiceResponse(false, "Неверный запрос!"))
    }
  }

  def deleteStatFromArticle(video: String, stat: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(video) & ObjectId.isValid(stat)) {
      val objectId = new ObjectId(video)
      articleRepository.getById(objectId).map {
        case _: Article =>
          articleRepository.deleteStatFromArticle(objectId, stat)
          ServiceResponse(false, s"Статистика успешна удалена")
        case _ => ServiceResponse(false, s"Статья $video не существует")
      }
    } else {
      Future(ServiceResponse(false, "Неверный запрос!"))
    }
  }

  def addStatToArticle(video: String, collectionId: String, percent: Int): Future[ServiceResponse] = {
    if (ObjectId.isValid(video) & ObjectId.isValid(collectionId)) {
      val objectIdVideo = new ObjectId(video)
      articleRepository.getById(objectIdVideo).map {
        case _: Article =>
          val stat = new Stat(collectionId, percent)
          articleRepository.addStatToArticle(objectIdVideo, stat)
          ServiceResponse(false, s"Статистика успешно добавлена")
        case _ =>
          ServiceResponse(false, s"Не удалось добавить статистику. Статья с id $video не найдено")
      }
    } else {
      Future(ServiceResponse(false, "Неверный запрос!"))
    }
  }

}
