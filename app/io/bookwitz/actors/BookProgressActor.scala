package io.bookwitz.actors

import java.io.File

import akka.actor.{Actor, ActorRef, Props}
import io.bookwitz.controllers.BookController
import io.bookwitz.models.BookWord
import play.api.libs.iteratee.Concurrent
import play.api.libs.json._

import scala.collection.mutable.ListBuffer

/**
  * Created by AVEKAUA on 7/06/2016.
  */
class BookProgressActor(file: File, userId: String, title: String) extends Actor with akka.actor.ActorLogging {

  var progressChannel: Concurrent.Channel[JsValue] = null
  var processActor: ActorRef = null
  var currentProgress = 0
  var error: Exception = null
  var status: String = "";
  var data: ListBuffer[BookWord] = ListBuffer[BookWord]()

  implicit val writer = new Writes[BookWord] {
    def writes(word: (BookWord)): JsValue = {
      Json.obj(
        "word" -> word.word,
        "tag" -> word.tag,
        "freq" -> word.freq,
        "definitions" -> Json.arr(word.definition),
        "pronunciations" -> Json.arr(word.pronunciation),
        "examples" -> Json.arr(word.example)
      )
    }
  }


  def receive = {
    case percent: Int => {
      if (currentProgress > -1)
        currentProgress = percent
    }

    case bookWords: ListBuffer[BookWord] => {
      data = bookWords
    }

    case errorMsg: Exception => {
      currentProgress = -1
      error = errorMsg
      status = "Failure."
    }

    case "storing" => {
      currentProgress = 0
      status = "Storing."
    }

    case "done" => {
      currentProgress = 100
      status = "Done."
    }

    case "progress" => {
      val result: JsValue = JsObject(
        Seq(
          "progress" -> JsNumber(currentProgress),
          "status" -> JsString(status),
          "data" -> Json.toJson(data)
        )
      )
      sender ! Json.stringify(result)
      if (currentProgress == 100) {
        context stop self
      }
    }

    case "start" => {
      processActor = BookController.system.actorOf(Props(new BookProcessActor(file, userId, title)))
      processActor ! 0
      status = "Processing."
    }
  }
}
