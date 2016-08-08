package io.bookwitz.actors

import java.io.{File, _}
import java.util._

import akka.actor.Actor
import edu.illinois.cs.cogcomp.edison.utilities.POSUtils
import edu.illinois.cs.cogcomp.nlp.lemmatizer.{MorphaStemmer, WordnetLemmaReader}
import edu.stanford.nlp.tagger.maxent.MaxentTagger
import io.bookwitz.models.BooksTableQueries.{bookWordsList, booksList}
import io.bookwitz.models.{Book, BookWord, Lemma}
import io.bookwitz.service.WordnikService
import play.api.Play.current
import play.api.db.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.slick.driver.JdbcDriver.simple._

//remove if not needed
import scala.collection.JavaConversions._

object BookProcessActor {

  private var tags: List[String] = new ArrayList[String]()

  private var verbLemmaMap: Map[String, String] = _

  private var verbBaseMap: Map[String, String] = _

  private var exceptionsMap: Map[String, String] = _

  private var wordNetPath: String = null

  tags.add("FW")

  tags.add("JJ")

  tags.add("NN")

  tags.add("RB")

  tags.add("VB")

  tags.add("VBN")

  tags.add("VBP")

  try {
    loadVerbMap(classOf[BookProcessActor].getClassLoader.getResource("resources/verb-lemDict.txt")
      .getFile)
    loadExceptionMap(classOf[BookProcessActor].getClassLoader.getResource("resources/exceptions.txt")
      .getFile)
    wordNetPath = classOf[BookProcessActor].getResource("/resources/WordNet-3.0/dict/")
      .getFile
  } catch {
    case e: Exception => System.out.print(e.toString)
  }

  private def loadExceptionMap(fileName: String) {
    exceptionsMap = new HashMap[String, String]()
    Source.fromFile(fileName).getLines().foreach {
      line =>
        if (line.length > 0) {
          val parts = line.split("\\s+")
          exceptionsMap.put(parts(0), parts(1))
        }
    }
  }

  private def loadVerbMap(fileName: String) {
    verbLemmaMap = new HashMap[String, String]()
    verbBaseMap = new HashMap[String, String]()
    Source.fromFile(fileName).getLines().foreach {
      line =>
        if (line.length > 0) {
          val parts = line.split("\\s+")
          val lemma = parts(0)
          verbBaseMap.put(lemma, lemma)
          for (i <- 1 until parts.length) verbLemmaMap.put(parts(i), lemma)
        }
    }
  }
}


class BookProcessActor(file: File, userId: String, title: String) extends Actor {

  private var wnLemmaReader: WordnetLemmaReader = _

  private var contractions: Map[String, String] = _

  private var toStanford: Map[String, String] = _

  private var useStanford: Boolean = false


  def processFile() = {
    if (BookProcessActor.verbLemmaMap == null || BookProcessActor.verbBaseMap == null || BookProcessActor.exceptionsMap == null ||
      BookProcessActor.wordNetPath == null) {
      sender ! new Exception("Wrong lemmatizer configuration");
    } else {
      sender ! "processing"
      try {
        val url = getClass.getClassLoader.getResource("resources/models/wsj-0-18-left3words-distsim.tagger")
        init()

        val tagger = new MaxentTagger(url.toString)
        val map = new HashMap[String, Lemma]()
        val fileSize = file.length
        var done = 0
        var currentPercent = 0
        Source.fromFile(file).getLines().foreach {
          s =>
            done += s.length
            val p = (done / (fileSize / 100 + 1)).toInt
            sender ! p
            val sentences = MaxentTagger.tokenizeText(new StringReader(s))
            for (sentence <- sentences) {
              val tSentence = tagger.tagSentence(sentence)
              for (word <- tSentence) {
                if (word.word().size > 1 && word.word().matches("[A-Za-z]+")) {
                  val tag = word.tag()
                  if (BookProcessActor.tags.contains(tag)) {
                    val lemmaStr = getLemma(word.word().toLowerCase(), tag).toLowerCase()
                    val lemmaWord = tagger.tagSentence(MaxentTagger.tokenizeText(new StringReader(lemmaStr))
                      .get(0))
                      .get(0)
                    var lemma = map.get(lemmaWord.toString)
                    if (lemma == null) {
                      lemma = new Lemma(lemmaWord)
                      map.put(lemmaWord.toString, lemma)
                    }
                    lemma.setCount(lemma.getCount + 1)
                  }
                }
              }
            }
        }
        file.delete()
        sender ! "storing"
        val bookId = Database.forDataSource(DB.getDataSource()) withSession { implicit session =>
          (booksList returning booksList.map(_.id)) += Book(None, title, userId)
        }
        var i = 0;
        map.foreach { case (key, value) => {
          WordnikService.getDictionaryEntry(value.getWord.word()).onComplete(
            result =>
              if (result.isSuccess && result.get.isDefined) {
                Database.forDataSource(DB.getDataSource()) withSession { implicit session =>
                  bookWordsList += BookWord(bookId.get, result.get.get, value.getWord.tag(), value.getCount)
                }
              }
          )
          i = i + 1
          sender ! (i / (map.size() / 100 + 1))
        }
        }

        sender ! 100
        context stop self
      } catch {
        case e: Exception => {
          sender ! e
          context stop self
        }
      }
    }
  }

  private def init() {
    wnLemmaReader = new WordnetLemmaReader(BookProcessActor.wordNetPath)
    contractions = new HashMap[String, String]()
    contractions.put("'d", "have")
    contractions.put("'ll", "will")
    contractions.put("'s", "'s")
    contractions.put("'re", "be")
    contractions.put("'m", "be")
    contractions.put("'ve", "have")
    toStanford = new HashMap[String, String]()
    toStanford.put("her", "she")
    toStanford.put("him", "he")
    toStanford.put("is", "be")
    toStanford.put("their", "they")
    toStanford.put("them", "they")
    toStanford.put("me", "i")
    toStanford.put("an", "a")
  }

  def getLemma(word: String, pos: String): String = {
    val posVerb = POSUtils.isPOSVerb(pos) || pos.startsWith("VB")
    val knownLemma = BookProcessActor.verbLemmaMap.containsKey(word)
    val contraction = contractions.containsKey(word)
    val exception = BookProcessActor.exceptionsMap.containsKey(word)
    if (exception) {
      return BookProcessActor.exceptionsMap.get(word)
    }
    val replaceRE = word.replace("re-", "")
    val knownTrimmedLemma = word.startsWith("re-") && BookProcessActor.verbLemmaMap.containsKey(replaceRE)
    var lemma: String = null
    if (word.indexOf('@') >= 0) {
      return word
    }
    if (pos.startsWith("V") && (word == "'s" || word == "’s")) {
      return "be"
    }
    if (useStanford && toStanford.containsKey(word)) {
      return toStanford.get(word)
    }
    if (contraction) {
      lemma = contractions.get(word)
      return lemma
    }
    if (pos == "NNP" || pos == "NNPS") {
      return word.toLowerCase()
    }
    if (pos.startsWith("JJ") && word.endsWith("ed")) return word
    if (pos == "JJR" || pos == "JJS" || pos == "RBR" || pos == "RBS" ||
      pos == "RB") {
      return word
    }
    if (posVerb && knownLemma) {
      lemma = if (pos == "VB") BookProcessActor.verbBaseMap.get(word) else BookProcessActor.verbLemmaMap.get(word)
      if (lemma != null) {
        if (lemma == "xmodal") return word
        return lemma
      }
    } else if (knownTrimmedLemma) {
      lemma = BookProcessActor.verbLemmaMap.get(replaceRE)
      if (lemma == "xmodal") return word
      if (lemma != null) return lemma
    }
    if (pos.startsWith("N") || pos.startsWith("J") || pos.startsWith("R") ||
      pos.startsWith("V")) {
      lemma = wnLemmaReader.getLemma(word, pos)
      if (lemma != null) return lemma
      if (word.endsWith("men")) {
        lemma = word.substring(0, word.length - 3) + "man"
        return lemma
      }
    } else {
      return word
    }
    if (word.endsWith("s") || pos.endsWith("S")) {
      lemma = MorphaStemmer.stem(word)
      return lemma
    }
    word
  }


  def receive = {
    case _ => {
      processFile
    }
  }
}