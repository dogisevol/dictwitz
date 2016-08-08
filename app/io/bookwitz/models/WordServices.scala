package io.bookwitz.models

import io.bookwitz.models.BooksTableQueries.servicesList

import scala.language.higherKinds
import scala.slick.driver.JdbcDriver.simple._

case class WordService(id: Option[Long], name: String, url: String, key: String) {
  def basicService(implicit session: Session): WordService = {
    val main = servicesList.filter(_.id === id).first
    WordService(main.id, main.name, main.url, main.key)
  }
}

class WordServices(tag: Tag) extends Table[WordService](tag, "wordService") {
  def id = column[Option[Long]]("rowid", O.PrimaryKey, O.AutoInc)

  def name = column[String]("name")

  def url = column[String]("url")

  def authKey = column[String]("key")

  def * = (id, name, url, authKey) <> (WordService.tupled, WordService.unapply)
}
