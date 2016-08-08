package io.dictwitz.actors

case class BookProcessMessage(text: Any, percent: Any)

case class BookProcessError(text: String)
