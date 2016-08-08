package io.bookwitz.models

import io.bookwitz.models.BooksTableQueries.bookWordsList

import scala.slick.driver.JdbcDriver.simple._

case class BookWord(bookId: Long, wordId: Long, tag: String, freq: Long) {
  def basicWord(implicit session: Session): BookWord = {
    val main = bookWordsList.filter(_.word === wordId).first

    BookWord(main.bookId, main.wordId, main.tag, main.freq)
  }
}

class BookWords(tag: Tag) extends Table[BookWord](tag, "bookWords") {
  def bookId = column[Long]("bookId", O.PrimaryKey)

  def word = column[Long]("wordId")

  def tagColumn = column[String]("tag")

  def freq = column[Long]("freq")

  def * = (bookId, word, tagColumn, freq) <>(BookWord.tupled, BookWord.unapply)
}
