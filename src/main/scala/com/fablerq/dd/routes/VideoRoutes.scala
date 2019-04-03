package com.fablerq.dd.routes

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.models.VideoParams
import com.fablerq.dd.services.VideoService
import com.fablerq.dd.configs.Json4sSupport._

class VideoRoutes(videoService: VideoService) {

  def route = {
    path("videos") {
      get {
        complete(videoService.getAllVideos)
      } ~
        post {
          entity(as[VideoParams]) { params =>
            complete(videoService.addVideo(params))
          }
        } ~
        patch {
          entity(as[VideoParams]) { params =>
            complete(videoService.updateVideoDescription(params))
          }
        } ~
        parameters("id".as[String]) { id =>
          post {
            complete(videoService.getVideo(id))
          } ~
            delete {
              complete(videoService.deleteVideo(id))
            }
        } ~
        parameters("video".as[String], "stat".as[String]) { (video, stat) =>
          delete {
            complete(videoService.deleteStatFromVideo(video, stat))
          }
        } ~
        parameters("video".as[String], "stat".as[String], "percent".as[Int]) { (video, stat, percent) =>
          post {
            complete(videoService.addStatToVideo(video, stat, percent))
          }
        }
    }
  }

}
