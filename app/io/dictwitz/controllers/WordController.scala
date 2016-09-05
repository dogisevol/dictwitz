package io.dictwitz.controllers

import io.dictwitz.service.WordnikService
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller}

import scala.language.postfixOps


class WordController() extends Controller {

  val logger = Logger(getClass)

  implicit val writer = new Writes[(Long, String, String, Long)] {
    def writes(t: (Long, String, String, Long)): JsValue = {
      Json.obj("bookId" -> t._1, "word" -> t._2, "tag" -> t._3, "freq" -> t._4)
    }
  }


  def getDictionaryEntry(word: String) = Action.async { request => {
    val future = WordnikService.getDictionaryEntry(word)

    future onFailure {
      case actorRef => {
        InternalServerError("failure")
      }
    }

    future.map(
      message =>
        Ok(message)
    )
  }
  }
}