package io.dictwitz.controllers


import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import io.dictwitz.actors.BookProgressActor
import io.dictwitz.models.BookWord
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller, MaxSizeExceeded}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps


object BookController {
  val system = ActorSystem("process")
}


class BookController() extends Controller {

  val logger = Logger(getClass)
  var progressChannel: Concurrent.Channel[JsValue] = null

  implicit val writer = new Writes[(Long, String, String, Long)] {
    def writes(t: (Long, String, String, Long)): JsValue = {
      Json.obj("bookId" -> t._1, "word" -> t._2, "tag" -> t._3, "freq" -> t._4)
    }
  }

  implicit val wordWriter = new Writes[(BookWord)] {
    def writes(t: (BookWord)): JsValue = {
      Json.obj("bookId" -> t., "word" -> t._2, "tag" -> t._3, "freq" -> t._4)
    }
  }


  def bookUpload = Action(parse.urlFormEncoded(maxLength = 1024 * 1024)) { request => {
    val (progressEnumerator, progressChannel) = Concurrent.broadcast[JsValue]
    val uuid = java.util.UUID.randomUUID().toString()
    if (!request.body.get("content").isEmpty) {
      BookController.system.actorOf(Props(new BookProgressActor(request.body.get("content").get.head)), uuid) ! "start"
      Ok(uuid);
    } else {
      BadRequest("Nothing to parse")
    }
  }
  }

  def bookProcessProgress(uuid: String) = Action.async { request => {
    val actorPath: ActorPath = BookController.system / uuid
    val actorSelection = BookController.system.actorSelection(actorPath)
    val future = actorSelection.resolveOne(10 second)

    future onFailure {
      case actorRef => {
        InternalServerError("failure")
      }
    }

    future.flatMap(
      response => {
        implicit val timeout = Timeout(5 seconds)
        val askFuture: Future[String] = ask(response, "progress").mapTo[String]
        askFuture.onFailure {
          case result =>
            InternalServerError("failure");
        }
        askFuture.map(
          message =>
            Ok(message.toString)
        )
      }
    )
  }
  }
}