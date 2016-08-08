package io.bookwitz.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import io.bookwitz.models.BooksTableQueries._
import io.bookwitz.models._
import play.api.Play.current
import play.api.db.DB
import play.api.db.slick.{Database => _}
import play.api.libs.json._

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.slick.driver.JdbcDriver.simple._

object WordnikService {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  private var wordService: WordService = _

  private def getRequest(word: String, operation: String, map: Map[String, String]): HttpRequest = {
    if (wordService == null) Database.forDataSource(DB.getDataSource()) withSession { implicit session =>
      wordService = servicesList
        .filter(_.name === "wordnik").firstOption.get
    }

    val url = Uri(wordService.url + word + "/" + operation)
      .withQuery(
        Uri.Query(
          "api_key" -> wordService.key,
          "limit" -> "10"
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
    val result: ListBuffer[String] = ListBuffer();
    (node \\ "text").foreach(
      text =>
        result += text.as[String]
    )
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

  def getDictionaryEntry(word: String): Future[Option[Long]] = {
    Database.forDataSource(DB.getDataSource()) withSession { implicit session =>
      dictionaryWordsList.filter(d => d.word === word).firstOption match {
        case Some(d) =>
          Future(d.id)
        case _ => {
          createDictionaryRecord(WordDictionary(None, word))
        }
      }
    }
  }

  def createDictionaryRecord(word: WordDictionary): Future[Option[Long]] = Future successful {
    val wordId = Database.forDataSource(DB.getDataSource()) withSession { implicit session =>
      (dictionaryWordsList returning dictionaryWordsList.map(_.id)) += word
    }

    Database.forDataSource(DB.getDataSource()) withSession { implicit session =>
      getDefinitions(word.word) onSuccess {
        case wordDefinitions => wordDefinitions.foreach(
          text =>
            wordDefinitionsList += WordDefinition(None, wordId.get, text)
        )
      }
    }

    Database.forDataSource(DB.getDataSource()) withSession { implicit session =>
      getPronunciations(word.word) onSuccess {
        case wordPronunciation =>
          if (wordPronunciation.isDefined)
            wordPronunciationList += WordPronunication(None, wordId.get, wordPronunciation.get)
      }
    }

    Database.forDataSource(DB.getDataSource()) withSession { implicit session =>
      getTopExample(word.word) onSuccess {
        case wordExample =>
          if (wordExample.isDefined)
            wordExamplesList += WordExample(None, wordId.get, wordExample.get)
      }
    }

    wordId
  }
}
