package io.bookwitz.models

import edu.stanford.nlp.ling.TaggedWord

class Lemma {
  private var word: TaggedWord = null
  private var count: Int = 0

  def this(word: TaggedWord) {
    this()
    this.setWord(word)
    this.setCount(0)
  }

  def getWord: TaggedWord = {
    return word
  }

  def setWord(word: TaggedWord) {
    this.word = word
  }

  def getCount: Int = {
    return count
  }

  def setCount(count: Int) {
    this.count = count
  }
}
