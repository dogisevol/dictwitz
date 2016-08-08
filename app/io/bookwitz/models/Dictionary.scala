package io.bookwitz.models

import scala.language.higherKinds
import scala.slick.driver.JdbcDriver.simple._

case class WordDictionary(id: Option[Long], word: String) {
}

class WordDictionaries(tag: Tag) extends Table[WordDictionary](tag, "dictionary") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def word = column[String]("word")

  def * = (id, word) <>(WordDictionary.tupled, WordDictionary.unapply)
}

case class WordDefinition(id: Option[Long], wordId: Long, text: String) {
}

class WordDefinitions(tag: Tag) extends Table[WordDefinition](tag, "dictDefinitions") {
  def id = column[Option[Long]]("rowid", O.PrimaryKey, O.AutoInc)

  def text = column[String]("text")

  def wordId = column[Long]("wordId")

  //  def word = foreignKey("wordId", wordId, dictionaryWordsList)(_.id)

  def * = (id, wordId, text) <>(WordDefinition.tupled, WordDefinition.unapply)
}

case class WordPronunication(id: Option[Long], wordId: Long, text: String) {
}

class WordPronunications(tag: Tag) extends Table[WordPronunication](tag, "dictPronunciations") {
  def id = column[Option[Long]]("rowid", O.PrimaryKey, O.AutoInc)

  def text = column[String]("text")

  def wordId = column[Long]("wordId")

  //  def word = foreignKey("wordId", wordId, dictionaryWordsList)(_.id)

  def * = (id, wordId, text) <>(WordPronunication.tupled, WordPronunication.unapply)
}

case class WordExample(id: Option[Long], wordId: Long, text: String) {
}

class WordExamples(tag: Tag) extends Table[WordExample](tag, "dictExamples") {
  def id = column[Option[Long]]("rowid", O.PrimaryKey, O.AutoInc)

  def text = column[String]("text")

  def wordId = column[Long]("wordId")

  //  def word = foreignKey("wordId", wordId, dictionaryWordsList)(_.id)

  def * = (id, wordId, text) <>(WordExample.tupled, WordExample.unapply)
}
