package io.bookwitz.controllers


import java.io.File

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import io.bookwitz.actors.BookProgressActor
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, Controller}

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


  def bookUpload = Action { request => {
    val (progressEnumerator, progressChannel) = Concurrent.broadcast[JsValue]
    val uuid = java.util.UUID.randomUUID().toString()
    val file = request.body.asMultipartFormData.get.files.head.ref.file
    val newFile = new File(file.getParentFile, uuid)
    file.renameTo(newFile)
    val title = request.body.asMultipartFormData.get.files.head.filename
    BookController.system.actorOf(Props(new BookProgressActor(newFile, request.queryString.get("userId").get.head, title)), uuid) ! "start"
    Ok(uuid);
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