package io.dictwitz.models

import scala.collection.mutable.ListBuffer

case class BookWord(word: String, tag: String, freq: Long, definition: ListBuffer[String],
                    pronunciation: ListBuffer[String], example: ListBuffer[String]) {
}
