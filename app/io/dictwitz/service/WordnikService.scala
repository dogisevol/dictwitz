package io.dictwitz.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import io.dictwitz.models._
import play.api.Logger
import play.api.libs.json._

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object WordnikService {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val logger = Logger(getClass)


  private def getRequest(word: String, operation: String, map: Map[String, String]): HttpRequest = {
    val url = Uri("http://api.wordnik.com:80/v4/word.json/" + word + "/" + operation)
      .withQuery(
        Uri.Query(
          "api_key" -> "a2a73e7b926c924fad7001ca3111acd55af2ffabf50eb4ae5",
          "limit" -> "3"
        )
      )
    HttpRequest(uri = url)
  }

  private def getResponse(word: String, operation: String): Future[JsValue] = {

    Http().singleRequest(getRequest(word, operation, Map())) flatMap {
      case response: HttpResponse =>
        response.entity.toStrict(5 seconds).map(_.data.decodeString("UTF-8")).map(result =>
          Json.parse(result))
    }
  }

  def transformDefinitions(node: JsValue): List[String] = {
    val result: ListBuffer[String] = ListBuffer()
    logger.debug("Transforming node: " + Json.stringify(node))
    (node \\ "text").foreach(
      text =>
        result += text.as[String]
    )
    logger.debug("Transforming node results: " + result)
    result.toList
  }


  def getDefinitions(word: String): Future[List[String]] = {
    getResponse(word, "definitions").map(
      node =>
        transformDefinitions(node)
    )
  }

  def getPronunciations(word: String): Future[Option[String]] = {
    getResponse(word, "pronunciations").map(
      node =>
        (node \\ "raw").head.asOpt[String]
    )
  }

  def getTopExample(word: String): Future[Option[String]] = {
    getResponse(word, "topExample").map(
      node =>
        (node \ "text").asOpt[String]
    )
  }

  def getDictionaryEntry(word: String): Future[BookWord] = {
    getDictionaryEntry(word, "", 0)
  }

  def getDictionaryEntry(lemma: Lemma): Future[BookWord] = {
    getDictionaryEntry(lemma.getWord.word(), lemma.getWord.tag(), lemma.getCount)
  }

  def getDictionaryEntry(word: String, tag: String, count: Long): Future[BookWord] = Future successful {
    val result = BookWord(word, tag, count, ListBuffer[String](),
      ListBuffer[String](), ListBuffer[String]())
    getDefinitions(word) onComplete {
      case Success(wordDefinitions) => {
        logger.debug("Word definitions: " + wordDefinitions)
        wordDefinitions.foreach(
          text => {
            result.definition += text
            logger.debug("Add definition: " + text)
            logger.debug("Book word: " + result.toString)
          }
        )
      }
      case Failure(t) =>
        logger.error("Cannot get definition for the word " + word, t)
    }

    logger.debug("Book word: " + result.toString)


    getPronunciations(word) onComplete {
      case Success(wordPronunciation) =>
        if (wordPronunciation.isDefined) {
          result.pronunciation += wordPronunciation.get
        }
      case Failure(t) =>
        logger.error("Cannot get definition for the word " + word, t)

    }

    getTopExample(word) onComplete {
      case Success(wordExample) =>
        if (wordExample.isDefined) {
          result.example += wordExample.get
        }
      case Failure(t) =>
        logger.error("Cannot get definition for the word " + word, t)

    }
    logger.debug("Book word: " + result.toString)
    result
  }

}
