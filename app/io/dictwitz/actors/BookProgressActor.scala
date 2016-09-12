package io.dictwitz.actors

import akka.actor.{Actor, ActorRef, Props}
import io.dictwitz.controllers.BookController
import play.api.libs.iteratee.Concurrent
import play.api.libs.json._

class BookProgressActor(content: String) extends Actor with akka.actor.ActorLogging {

  var progressChannel: Concurrent.Channel[JsValue] = null
  var processActor: ActorRef = null
  var currentProgress = 0
  var error: Exception = null
  var status: String = "";
  var data: JsArray = JsArray()

  def receive = {
    case percent: Int => {
      if (currentProgress > -1)
        currentProgress = percent
    }

    case bookWords: JsArray => {
      data = bookWords
      status = "done"
    }

    case errorMsg: Exception => {
      currentProgress = -1
      error = errorMsg
      status = "failure"
    }

    case "storing" => {
      currentProgress = 0
      status = "storing"
    }

    case "progress" => {
      if (error != null) {
        val result: JsValue = JsObject(
          Seq(
            "progress" -> JsNumber(currentProgress),
            "status" -> JsString(status),
            "error" -> JsString(error.getMessage)
          )
        )
        sender ! Json.stringify(result)
        context stop self
      } else if (currentProgress < 100) {
        val result: JsValue = JsObject(
          Seq(
            "progress" -> JsNumber(currentProgress),
            "status" -> JsString(status)
          )
        )
        sender ! Json.stringify(result)

      } else {
        val result: JsValue = JsObject(
          Seq(
            "progress" -> JsNumber(currentProgress),
            "status" -> JsString(status),
            "data" -> data
          )
        )
        sender ! Json.stringify(result)
        if (currentProgress == 100) {
          context stop self
        }
      }
    }

    case "start" => {
      processActor = BookController.system.actorOf(Props(new BookProcessActor(content)))
      processActor ! 0
      status = "processing"
    }
  }
}
