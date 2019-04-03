package com.fablerq.dd.services

import com.fablerq.dd.models._
import com.fablerq.dd.repositories.VideoRepository
import org.bson.types.ObjectId
import org.mongodb.scala.bson.ObjectId

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class VideoService(videoRepository: VideoRepository) {

  def getAllVideos: Future[Either[ServiceResponse, Seq[Video]]] = {
    videoRepository.getAll.map {
      case x: Seq[Video] if x.nonEmpty => Right(x)
      case _ => Left(ServiceResponse(false, "База данных видео пуста"))
    }
  }

  def getVideo(id: String): Future[Either[ServiceResponse, Video]] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      videoRepository.getById(objectId).map {
        case video: Video => Right(video)
        case _ => Left(ServiceResponse(false, "Видео не найдено!"))
      }
    } else {
      Future(Left(ServiceResponse(false, "Неверный запрос!")))
    }
  }

  def addVideo(params: VideoParams): Future[ServiceResponse] = params match {
    case VideoParams(Some(title), Some(description), Some(link), None) =>
      videoRepository.getByLink(link).map {
        case _: Video =>
          ServiceResponse(false, s"Видео по этой ссылке ${params.link} уже добавлено")
        case _ =>
          videoRepository.addVideo(Video.apply(
            new ObjectId(), title,
            description, link, List()
          ))
          ServiceResponse(true, "Видео успешно успешно добавлено")
      }
    case _ => Future(ServiceResponse(false, s"Видео ${params.title} не удалось добавить. Неверный запрос"))
  }

  def updateVideoDescription(params: VideoParams): Future[ServiceResponse] = params match {
    case VideoParams(Some(title), Some(description), None, None) =>
      videoRepository.getByTitle(title).map {
        case video: Video =>
          videoRepository.updateVideoDescription(video._id, params.description.get)
          ServiceResponse(true, "Описание видео успешно обновлено")
        case _ => ServiceResponse(false, s"Видео $title не найдено")
      }
    case _ => Future(ServiceResponse(false, s"Видео ${params.title} не удалось обновить. Неверный запрос"))
  }

  def deleteVideo(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      videoRepository.getById(objectId).map {
        case video: Video =>
          videoRepository.deleteVideo(video._id)
          ServiceResponse(true, "Видео успешно удалено")
        case _ => ServiceResponse(false, "Не удалось удалить видео")
      }
    } else {
      Future(ServiceResponse(false, "Неверный запрос!"))
    }
  }

  def deleteStatFromVideo(video: String, stat: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(video) & ObjectId.isValid(stat)) {
      val objectId = new ObjectId(video)
      videoRepository.getById(objectId).map {
        case _: Video =>
          videoRepository.deleteStatFromVideo(objectId, stat)
          ServiceResponse(false, s"Статистика успешна удалена")
        case _ => ServiceResponse(false, s"Видео $video не существует")
      }
    } else {
      Future(ServiceResponse(false, "Неверный запрос!"))
    }
  }

  def addStatToVideo(video: String, collectionId: String, percent: Int): Future[ServiceResponse] = {
    if (ObjectId.isValid(video) & ObjectId.isValid(collectionId)) {
      val objectIdVideo = new ObjectId(video)
      videoRepository.getById(objectIdVideo).map {
        case _: Video =>
          val stat = new Stat(collectionId, percent)
          videoRepository.addStatToVideo(objectIdVideo, stat)
          ServiceResponse(false, s"Статистика успешно добавлена")
        case _ =>
          ServiceResponse(false, s"Не удалось добавить статистику. Видео с id $video не найдено")
      }
    } else {
      Future(ServiceResponse(false, "Неверный запрос!"))
    }
  }

}
