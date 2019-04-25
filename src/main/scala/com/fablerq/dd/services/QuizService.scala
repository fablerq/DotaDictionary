package com.fablerq.dd.services

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

import com.fablerq.dd.models.{Question, Quiz, QuizParams, ServiceResponse}
import com.fablerq.dd.repositories.QuizRepository
import org.bson.types.ObjectId
import org.mongodb.scala.bson.ObjectId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

trait QuizService {
  def getAllQuizzes: Future[Either[ServiceResponse, Seq[Quiz]]]
  def getQuiz(id: String): Future[Either[ServiceResponse, Quiz]]
  def getNumberOfQuestions(id: String): Future[ServiceResponse]
  def getQuizzesByPage(page: Int): Future[Either[ServiceResponse, Seq[Quiz]]]
  def getCountOfQuizzes: Future[ServiceResponse]
  def getQuizByTitle(title: String): Future[Option[Quiz]]
  def getByIdDirectly(id: ObjectId): Future[Option[Quiz]]
  def addQuiz(params: QuizParams): Future[ServiceResponse]
  def deleteQuiz(id: String): Future[ServiceResponse]
  def startQuiz(
          collectionId: String,
          quizType: Int,
          level: String): Future[ServiceResponse]
  def setQuestionsForQuiz(
          questions: List[((String, Int), Int)]): Future[List[(Question, String)]]
  def playQuiz(quizId: String): Future[Either[ServiceResponse, Question]]
  def continueQuiz(
          quizId: String,
          step: Int,
          answer: String): Future[ServiceResponse]
  def doneQuiz(quizId: String): Future[ServiceResponse]
  def matchType0(data: ((String, Int), Int)): Future[(Question, String)]
  def matchType1(data: ((String, Int), Int)): Future[(Question, String)]
  def matchType2(data: ((String, Int), Int)): Future[(Question, String)]
  def matchType3(data: ((String, Int), Int)): Future[(Question, String)]
  def matchType4(data: ((String, Int), Int)): Future[(Question, String)]
  def unwrapFutureList[A](list: List[Future[A]]): Future[List[A]]
}

class QuizServiceImpl(
           quizRepository: QuizRepository,
           collectionService: WordCollectionService,
           mainService: MainService,
           wordService: WordService) extends QuizService {

  def getAllQuizzes: Future[Either[ServiceResponse, Seq[Quiz]]] = {
    quizRepository.getAll.map {
      case x: Seq[Quiz] if x.nonEmpty => Right(x)
      case _ =>
        Left(ServiceResponse(false, "База данных квизов пуста"))
    }
  }

  def getQuiz(id: String): Future[Either[ServiceResponse, Quiz]] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      quizRepository.getById(objectId).map {
        case quiz: Quiz => Right(quiz)
        case _ =>
          Left(ServiceResponse(false, "Квиз не найден!"))
      }
    } else Future(Left(ServiceResponse(false, "Неверный запрос!")))
  }

  def getNumberOfQuestions(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      quizRepository.getById(objectId).map {
        case quiz: Quiz =>
          ServiceResponse(false, quiz.totalSteps.toString)
        case _ =>
          ServiceResponse(false, "Квиз не найден!")
      }
    } else Future(ServiceResponse(false, "Неверный запрос!"))
  }

  def getQuizzesByPage(page: Int): Future[Either[ServiceResponse, Seq[Quiz]]] = {
    quizRepository.getAll.map {
      case x: Seq[Quiz] if x.nonEmpty =>
        Right(x
          .sortBy(_.title)
          .slice((page - 1) * 10, page * 10))
      case _ =>
        Left(ServiceResponse(false, "База данных квизов пуста"))
    }
  }

  def getCountOfQuizzes: Future[ServiceResponse] = {
    quizRepository.count.map{ x =>
      ServiceResponse(true, x.toString)
    }
  }

  def getQuizByTitle(title: String): Future[Option[Quiz]] = {
    quizRepository.getByTitle(title).map { x =>
      Option(x)
    }
  }

  def getByIdDirectly(id: ObjectId): Future[Option[Quiz]] =
    quizRepository.getById(id).map { x =>
      Option(x)
    }

  def addQuiz(params: QuizParams): Future[ServiceResponse] = params match {
    case QuizParams(
      Some(title), Some(collection), Some(level), Some(date), Some(questions),
      Some(answers), Some(quizType), None, Some(totalSteps), None, None) =>
        val id = new ObjectId()
        quizRepository.addQuiz(Quiz.apply(
          id, title, collection, level, date, questions,
          answers, quizType, 0, totalSteps, 0, false
        )).map { x =>
          ServiceResponse(true, id.toString)
        }
    case _ =>
      Future.successful(
        ServiceResponse(false,
          s"Квиз ${params.title} не удалось добавить.Неверный запрос"))
  }

  def deleteQuiz(id: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(id)) {
      val objectId = new ObjectId(id)
      getByIdDirectly(objectId).flatMap {
        case Some(x) =>
          quizRepository.deleteQuiz(x._id).map { x =>
            ServiceResponse(true, "Квиз успешно удален")
          }
        case None =>
          Future.successful(ServiceResponse(false, "Не удалось удалить квиз"))
      }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }

  def startQuiz(
                 collectionId: String,
                 quizType: Int,
                 level: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(collectionId)) {
      val objectId = new ObjectId(collectionId)
        collectionService.getByIdDirectly(objectId).flatMap {
          case Some(y) =>
            if (quizType != 0 && quizType != 1) {
              Future.successful(
                ServiceResponse(false, s"Не существует такого типа квиза"))
            }
            else {
              val totalSteps: Int = level match {
                case "Легкий" => 10
                case "Средний" => 20
                case _ => 30
              }
              println("totalsteps="+totalSteps)

              val date: String =
                LocalDateTime
                  .now(ZoneId.of("Europe/Moscow"))
                  .format(DateTimeFormatter.ofPattern("d-M-y h:m"))

              //((Word, Type of answer), Index of step)
              val questions: List[((String, Int), Int)] =
                Random.shuffle(y.words)
                  .take(totalSteps)
                  .map(x => x -> (Random.nextInt(4) + quizType))
                .zipWithIndex

              setQuestionsForQuiz(questions).flatMap { data =>
                quizRepository.count.flatMap { numberOfQuizzes =>
                  val params: QuizParams =
                    QuizParams(
                      Some(s"Quiz № ${numberOfQuizzes+1}"), Some(y.title),
                      Some(level), Some(date), Some(data.map(x => x._1)),
                      Some(data.map(x => x._2)), Some(quizType),
                      None, Some(totalSteps), None, None)

                  addQuiz(params).map { x =>
                    if (x.bool)
                      ServiceResponse(true, x.message)
                    else
                      ServiceResponse(false, s"Не удалось добавить квиз!")
                  }
                }
              }

            }
          case None =>
            Future.successful(
              ServiceResponse(false, s"Такой коллекции не существует"))
        }
    } else Future.successful(ServiceResponse(false, "Неверный запрос!"))
  }


  def setQuestionsForQuiz(
    questions: List[((String, Int), Int)]): Future[List[(Question, String)]] = {
    val result: List[Future[(Question,String)]] = questions.map { x =>
      x._1._2 match {
        case 0 => matchType0(x)
        case 1 => matchType1(x)
        case 2 => matchType2(x)
        case 3 => matchType3(x)
        case 4 => matchType4(x)
      }
    }
    unwrapFutureList(result)
  }

  def playQuiz(quizId: String): Future[Either[ServiceResponse, Question]] = {
    if (ObjectId.isValid(quizId)) {
      val objectId = new ObjectId(quizId)
      getByIdDirectly(objectId).flatMap {
        case None =>
          Future.successful(Left(ServiceResponse(false, s"Квиз не найден")))
        case Some(quiz) =>
          val question: Question =
            quiz.questions.find(_.step == quiz.doneSteps).get
          Future.successful(Right(question))
      }
    }
    else
      Future.successful(Left(ServiceResponse(false, s"Неверный запрос!")))
  }

  def continueQuiz(
                  quizId: String,
                  step: Int,
                  answer: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(quizId)) {
      val objectId = new ObjectId(quizId)
      getByIdDirectly(objectId).flatMap {
        case Some(quiz) =>
          quiz.isClose match {
            case true =>
              Future.successful(ServiceResponse(false,
                s"Ошибка! Квиз уже был пройден"))
            case false =>
              if (0 <= step && step <= quiz.totalSteps) {
                val question: Question = quiz.questions.find(_.step == step).get
                //check if answer has already given
                question.userAnswer match {
                  case None =>
                    val isTrue: Int = answer match {
                      case x if x == quiz.answers.reverse(step) => 1
                      case _ => 0
                    }
                    val updatedQuestion = Question.apply(
                      title = question.title,
                      questionType = question.questionType,
                      responseOptions = question.responseOptions,
                      step = question.step,
                      userAnswer = Some(answer),
                      audioTitle = question.audioTitle)

                    quizRepository.updateQuizScore(objectId, quiz.score + isTrue)
                      .flatMap { _ =>
                        quizRepository.updateQuizDoneSteps(objectId, quiz.doneSteps + 1)
                          .flatMap { _ =>
                            quizRepository.deleteQuestion(objectId, step)
                              .flatMap { _ =>
                                quizRepository.addQuestion(objectId, updatedQuestion)
                                  .flatMap { _ =>
                                    isTrue match {
                                      case 1 =>
                                        Future.successful(ServiceResponse(true, "Верно!"))
                                      case 0 =>
                                        Future.successful(ServiceResponse(true, "Ошибка!"))
                                    }
                                  }
                              }
                          }
                      }
                  case Some(x) =>
                    Future.successful(ServiceResponse(false, s"Ошибка! Вы уже " +
                      s"отвечали на данный пункт"))
                }
              }
              else
                Future.successful(ServiceResponse(false, s"Ошибка! Неверный шаг квиза"))
          }
        case None =>
          Future.successful(ServiceResponse(false, s"Квиз не найден"))
      }
    }
    else
      Future.successful(ServiceResponse(false, s"Неверный запрос!"))
  }

  def doneQuiz(quizId: String): Future[ServiceResponse] = {
    if (ObjectId.isValid(quizId)) {
      val objectId = new ObjectId(quizId)
      getByIdDirectly(objectId).flatMap {
        case Some(quiz) =>
          quiz.isClose match {
            case true =>
              Future.successful(ServiceResponse(false, s"Квиз уже пройден"))
            case false =>
              quizRepository.updateQuizStatus(objectId).flatMap{ _ =>
                Future.successful(ServiceResponse(true,
                  s"Квиз успешно пройден. Ваш результат: ${quiz.score} из " +
                    s"${quiz.totalSteps}"))
              }
          }
        case None =>
          Future.successful(ServiceResponse(false, s"Квиз не найден"))
      }
    }
    else
      Future.successful(ServiceResponse(false, s"Неверный запрос!"))
  }


  //en-rus without options
  def matchType0(data: ((String, Int), Int)): Future[(Question, String)] = {
    mainService.translateWord(data._1._1).map { word =>
      (Question.apply(
        title = s"Как перевести слово '${data._1._1}'?",
        questionType = 0,
        responseOptions = None,
        step = data._2,
        userAnswer = None,
        audioTitle = None),
      word)
    }

  }

  //en-rus with options
  def matchType1(data: ((String, Int), Int)): Future[(Question, String)] = {
    wordService.getAllWords.flatMap {
      case Right(value) =>
        val shuffleNumber: Int = value.length match {
          case x if x > 20 => 50
          case _ => value.length
        }
        val elements: List[String] =
          Random.shuffle(value.take(shuffleNumber))
            .take(3)
            .toList
            .map(x => x.title)

        val translatedElements: List[Future[String]] = elements.map { word =>
          mainService.translateWord(word)
        }

        //converted List[Future[String]] to Future[List[String]]
        val finalTranslatedElements: Future[List[String]] =
          unwrapFutureList(translatedElements)

        finalTranslatedElements.flatMap { finalElements =>
          mainService.translateWord(data._1._1).map { translated =>
            val shuffledElements: List[String] =
              Random.shuffle(finalElements :+ translated)
            (Question.apply(
              title = s"Как перевести слово '${data._1._1}'?",
              questionType = 1,
              responseOptions = Some(shuffledElements),
              step = data._2,
              userAnswer = None,
              audioTitle = None),
            translated)
          }

        }

    }
  }

  //rus-en without options
  def matchType2(data: ((String, Int), Int)): Future[(Question, String)] = {
    mainService.translateWord(data._1._1).map { word =>
      (Question.apply(
        title = s"Как на английском будет слово '$word'?",
        questionType = 2,
        responseOptions = None,
        step = data._2,
        userAnswer = None,
        audioTitle = None),
       data._1._1)
    }
  }

  //rus-en with options
  def matchType3(data: ((String, Int), Int)): Future[(Question, String)] = {
    mainService.translateWord(data._1._1).flatMap { word =>
      wordService.getAllWords.map {
        case Right(value) =>
          val shuffleNumber: Int = value.length match {
            case x if x > 20 => 50
            case _ => value.length
          }
          val elements: List[String] =
            Random.shuffle(value.take(shuffleNumber))
              .take(3)
              .toList
              .map(x => x.title)

          val shuffledElements: List[String] =
            Random.shuffle(elements :+ data._1._1)

          (Question.apply(
            title = s"Как на английском будет слово '$word'?",
            questionType = 3,
            responseOptions = Some(shuffledElements),
            step = data._2,
            userAnswer = None,
            audioTitle = None),
           data._1._1)
      }
    }
  }

  //en-rus with audio
  def matchType4(data: ((String, Int), Int)): Future[(Question, String)] = {
    mainService.translateWord(data._1._1).map { word =>
      (Question.apply(
        title = s"Прослушайте слово. Как оно переводится?",
        questionType = 4,
        responseOptions = None,
        step = data._2,
        userAnswer = None,
        audioTitle = Some(data._1._1)),
      word)
    }
  }

  def unwrapFutureList[A](list: List[Future[A]]): Future[List[A]] = {
    list
      .map(_.map(Some(_)).recover { case _ => None })
      .foldLeft(Future.successful(List.empty[A])) {
        case (accFuture, nextFuture) =>
          nextFuture.zip(accFuture)
            .map {
              case (Some(next), acc) => next :: acc
              case (_, acc) => acc
            }
      }
  }





}
