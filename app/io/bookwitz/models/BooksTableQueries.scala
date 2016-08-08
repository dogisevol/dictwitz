package io.bookwitz.models

import scala.slick.lifted.TableQuery

object BooksTableQueries {

  object booksList extends TableQuery(new Books(_))

  object bookWordsList extends TableQuery(new BookWords(_))

  object servicesList extends TableQuery(new WordServices(_))

  object dictionaryWordsList extends TableQuery(new WordDictionaries(_))

  object wordDefinitionsList extends TableQuery(new WordDefinitions(_))

  object wordPronunciationList extends TableQuery(new WordPronunications(_))

  object wordExamplesList extends TableQuery(new WordExamples(_))

}
