package io.dictwitz.controllers

import io.dictwitz.service.WordnikService
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.language.postfixOps


class WordController() extends Controller {

  val logger = Logger(getClass)

  def getDictionaryEntry(word: String) = Action.async { request => {
    val future = WordnikService.getDictionaryEntry(word)
    future.map(word => {
      logger.debug("Ready to send the word: " + word)
      val result = Json.obj(
        "word" -> word.word,
        "definitions" -> Json.arr(word.definition),
        "pronunciations" -> Json.arr(word.pronunciation),
        "examples" -> Json.arr(word.example)
      )
      Ok(Json.stringify(result))
    }).recover {
      case _ =>
        InternalServerError
    }
  }
  }
}