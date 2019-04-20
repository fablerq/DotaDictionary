package com.fablerq.dd.services

import com.fablerq.dd.models._
import com.fablerq.dd.repositories.VideoRepository
import org.bson.types.ObjectId
import org.mongodb.scala.bson.ObjectId

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait VideoService {
  def getAllVideos: Future[Either[ServiceResponse, Seq[Video]]]
  def getVideo(id: String): Future[Either[ServiceResponse, Video]]
  def getIdByLink(link: String): Future[Option[Video]]
  def getByIdDirectly(id: ObjectId): Future[Option[Video]]
  def addVideo(params: VideoParams): Future[ServiceResponse]
  def addVideoDirectly(video: Video): Future[Boolean]
  def updateVideoDescription(params: VideoParams): Future[ServiceResponse]
  def deleteVideo(id: String): Future[ServiceResponse]
  //stats for video
  def deleteStatFromVideo(video: String, stat: String): Future[ServiceResponse]
  def addStatToVideo(video: String, collectionId: String, percent: Int): Future[ServiceResponse]
}

class VideoServiceImpl(videoRepository: VideoRepository) extends VideoService {

  def getAllVideos: Future[Either[ServiceResponse, Seq[Video]]] = {
    videoRepository.getAll.map {
      case x: Seq[Video] if x.nonEmpty => Right(x)
      case _ =>
        Left(ServiceResponse(false, "База данных видео пуста"))
    }
  }

  def getVideo(id: String): Future[Either[ServiceResponse, Video]] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          val words: List[WordStat] = x.words.sortBy(-_.count)
          Future.successful(Right(Video(
            x._id,
            x.title,
            x.description,
            words,
            x.link,
            x.stats)))
        case _ =>
          Future.successful(
            Left(ServiceResponse(false, "Видео не найдено!")))
      }
    } else Future.successful(Left(ServiceResponse(false, "Неверный запрос!")))
  }

  def getIdByLink(link: String): Future[Option[Video]] =
    videoRepository.getByLink(link).map { x=>
      Option(x)
    }

  def getByIdDirectly(id: ObjectId): Future[Option[Video]] =
    videoRepository.getById(id).map { x=>
      Option(x)
    }

  def addVideo(params: VideoParams): Future[ServiceResponse] = params match {
    case VideoParams(Some(title), Some(description), Some(link), None) =>
      getIdByLink(link).flatMap {
        case Some(x) =>
          Future.successful(
            ServiceResponse(false, s"Видео по этой ссылке ${params.link} уже добавлено"))
        case None =>
          videoRepository.addVideo(Video.apply(
            new ObjectId(), title,
            description, List(), link, List()))
              .map { x=>
                ServiceResponse(true, "Видео успешно добавлено")
              }
      }
    case _ => Future.successful(ServiceResponse(false, s"Видео ${params.title} " +
      s"не удалось добавить. Неверный запрос"))
  }

  def addVideoDirectly(video: Video): Future[Boolean] = {
    videoRepository.addVideo(video).map(_ => true)
  }

  def updateVideoDescription(params: VideoParams): Future[ServiceResponse] = params match {
    case VideoParams(Some(title), Some(description), Some(link), None) =>
      getIdByLink(link).flatMap {
        case Some(x) =>
          videoRepository.updateVideoDescription(x._id, params.description.get).map { x=>
            ServiceResponse(true, "Описание видео успешно обновлено")
          }
        case None =>
          Future.successful(ServiceResponse(false, s"Видео $title не найдено"))
      }
    case _ =>
      Future.successful(ServiceResponse(false, s"Видео ${params.title} не удалось " +
        s"обновить. Неверный запрос"))
  }

  def deleteVideo(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          videoRepository.deleteVideo(x._id).map { x=>
            ServiceResponse(true, "Видео успешно удалено")
          }
        case None =>
          Future.successful(ServiceResponse(false, "Не удалось удалить видео"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

  def deleteStatFromVideo(video: String, collectionId: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(video) & ObjectId.isValid(collectionId)) {
      val objectId = new ObjectId(video)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          if (x.stats.exists(_.collectionId == collectionId))
            videoRepository.deleteStatFromVideo(objectId, collectionId).map { x=>
              ServiceResponse(false, s"Статистика успешна удалена")
            }
          else
            Future.successful(ServiceResponse(false, s"Статистика в видео отсутствует"))
        case None =>
          Future.successful(ServiceResponse(false, s"Видео $video не существует"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

  def addStatToVideo(
    video: String,
    collectionId: String,
    percent: Int): Future[ServiceResponse] = {
    if (ObjectId.isValid(video) & ObjectId.isValid(collectionId)) {
      val objectIdVideo = new ObjectId(video)
      getByIdDirectly(objectIdVideo).flatMap {
        case Some(x) =>
          if (x.stats.exists(_.collectionId == collectionId))
            Future.successful(
              ServiceResponse(false, s"Статистика в видео уже присутствует"))
          else {
            val stat = new Stat(collectionId, percent)
            videoRepository.addStatToVideo(objectIdVideo, stat)
              .map { _ =>
                ServiceResponse(false, s"Статистика успешно добавлена")
              }
          }
        case None =>
          Future.successful(
            ServiceResponse(false, s"Не удалось добавить статистику. " +
              s"Видео с id $video не найдено"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

}
