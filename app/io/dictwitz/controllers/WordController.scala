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
    WordnikService.getDictionaryEntry(word).map(word => {
      val result = Json.obj(
        "word" -> word.word,
        "definitions" -> word.definition,
        "pronunciations" -> word.pronunciation,
        "examples" -> word.example
      )
      Ok(Json.stringify(result))
    }).recover {
      case e: Exception =>
        logger.error("Cannot get the definitions", e)
        InternalServerError
    }
  }
  }
}