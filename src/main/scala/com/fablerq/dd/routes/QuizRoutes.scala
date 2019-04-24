package com.fablerq.dd.routes

import akka.http.scaladsl.server.Directives.{as, complete, delete, entity, get, parameters, patch, path, post}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import com.fablerq.dd.models.QuizParams
import com.fablerq.dd.services.QuizService
import akka.http.scaladsl.server.Directives._
import com.fablerq.dd.configs.Json4sSupport._
import com.fablerq.dd.models.WordParams


class QuizRoutes(quizService: QuizService) {

  def route = cors() {
    path("quizzes") {
      get {
        complete(quizService.getAllQuizzes)
      } ~
        post {
          entity(as[QuizParams]) { params =>
            complete(quizService.addQuiz(params))
          }
        } ~
        parameters("id".as[String]) { id =>
          delete {
            complete(quizService.deleteQuiz(id))
          } ~
            post {
              complete(quizService.getQuiz(id))
            }
        } ~
        parameters(
          "playQuizId".as[String]) {
          playQuizId =>
            post {
              complete(quizService.playQuiz(playQuizId))
            }
        } ~
        parameters(
          "collectionId".as[String],
          "quizType".as[Int],
          "level".as[String]) {
          (collectionId, quizType, level) =>
            post {
              complete(quizService.startQuiz(collectionId, quizType, level))
            }
        } ~
        parameters(
          "quizId".as[String],
          "step".as[Int],
          "answer".as[String]) {
          (quizId, step, answer) =>
            post {
              complete(quizService.continueQuiz(quizId, step, answer))
            }
        } ~
        parameters(
          "doneQuizId".as[String]) {
          doneQuizId =>
            post {
              complete(quizService.doneQuiz(doneQuizId))
            }
        }
    } ~
    path("countquizzes") {
      post {
        complete(quizService.getCountOfQuizzes)
      } ~
      parameters("page".as[Int]) {
        page =>
          post {
            complete(quizService.getQuizzesByPage(page))
          }
      } ~
        parameters("id".as[String]) {
          id =>
            post {
              complete(quizService.getNumberOfQuestions(id))
            }
        }
    }

  }
}
