package com.fablerq.dd.routes

import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.models.VideoParams
import com.fablerq.dd.services.VideoService
import com.fablerq.dd.configs.Json4sSupport._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

class VideoRoutes(videoService: VideoService) {

  def route = cors() {
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
        }
    } ~
    path("countvideos") {
      parameters("id".as[String], "page".as[Int]) {
        (id, page) =>
          post {
            complete(videoService.getWordsByPage(id, page))
          }
      } ~
      parameters("id".as[String]) {
        id =>
          post {
            complete(videoService.getCountOfWords(id))
          }
      }
    }
  }
}
