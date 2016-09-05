package io.dictwitz.controllers

import io.dictwitz.models.BookWord
import io.dictwitz.service.WordnikService
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller}

import scala.language.postfixOps


class WordController() extends Controller {

  val logger = Logger(getClass)

  def getDictionaryEntry(word: String) = Action.async { request => {
    val future = WordnikService.getDictionaryEntry(word)

    future onFailure {
      case actorRef => {
        InternalServerError("failure")
      }
    }

    future.map(
      word =>
        Ok(      Json.obj(
          "word" -> word.word,
          "definitions" -> Json.arr(word.definition),
          "pronunciations" -> Json.arr(word.pronunciation),
          "examples" -> Json.arr(word.example)
        ))
    )
  }
  }
}