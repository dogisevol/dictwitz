package io.bookwitz.models


import io.bookwitz.models.BooksTableQueries.booksList

import scala.language.higherKinds
import scala.slick.driver.JdbcDriver.simple._

case class Book(id: Option[Long], title: String, userId: String) {
  def basicBook(implicit session: Session): Book = {
    val main = booksList.filter(_.id === id).first
    Book(main.id, main.title, main.userId)
  }
}

class Books(tag: Tag) extends Table[Book](tag, "books") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def title = column[String]("title")

  def userId = column[String]("userId")

  def * = (id, title, userId) <>(Book.tupled, Book.unapply)
}
