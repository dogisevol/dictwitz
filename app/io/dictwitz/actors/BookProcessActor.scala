package io.dictwitz.actors

import java.io.{File, _}
import java.util._

import akka.actor.Actor
import edu.illinois.cs.cogcomp.edison.utilities.POSUtils
import edu.illinois.cs.cogcomp.nlp.lemmatizer.{MorphaStemmer, WordnetLemmaReader}
import edu.stanford.nlp.tagger.maxent.MaxentTagger
import io.dictwitz.models.{BookWord, Lemma}
import play.Play
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.io.Source

object BookProcessActor {

  val logger = Logger(getClass)

  private var tags: List[String] = new ArrayList[String]()

  private var verbLemmaMap: Map[String, String] = _

  private var verbBaseMap: Map[String, String] = _

  private var exceptionsMap: Map[String, String] = _

  private var wordNetPath: String = null

  private var tagger: MaxentTagger = null

  tags.add("FW")
  tags.add("JJ")
  tags.add("NN")
  tags.add("RB")
  tags.add("VB")
  tags.add("VBN")
  tags.add("VBP")

  try {
    loadVerbMap(Play.application().getFile("resources/verb-lemDict.txt"))
    loadExceptionMap(Play.application().getFile("resources/exceptions.txt"))
    wordNetPath = Play.application().getFile("resources/WordNet-3.0/dict/")
      .getAbsolutePath
    tagger = new MaxentTagger(Play.application().getFile("resources/models/wsj-0-18-left3words-distsim.tagger").toString)
  } catch {
    case e: Exception => logger.error("Cannot initialize lemmatizer", e)
  }

  private def loadExceptionMap(file: File) {
    logger.info("Start loading exeptions map: " + file.getAbsolutePath)
    exceptionsMap = new HashMap[String, String]()
    Source.fromFile(file).getLines().foreach {
      line =>
        if (line.length > 0) {
          val parts = line.split("\\s+")
          exceptionsMap.put(parts(0), parts(1))
        }
    }
    logger.info("exeptions map: " + exceptionsMap)
  }

  private def loadVerbMap(file: File) {
    logger.info("Start loading verb maps: " + file.getAbsolutePath)
    verbLemmaMap = new HashMap[String, String]()
    verbBaseMap = new HashMap[String, String]()
    Source.fromFile(file).getLines().foreach {
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


class BookProcessActor(content: String) extends Actor {

  val logger = Logger(getClass)

  private var wnLemmaReader: WordnetLemmaReader = _

  private var contractions: Map[String, String] = _

  private var toStanford: Map[String, String] = _

  private var useStanford: Boolean = false


  def processFile() = {
    if (BookProcessActor.verbLemmaMap == null || BookProcessActor.verbBaseMap == null || BookProcessActor.exceptionsMap == null ||
      BookProcessActor.wordNetPath == null) {
      sender ! new Exception("Cannot configure lemmatizer")
      if (BookProcessActor.verbLemmaMap == null)
        logger.error("Wrong lemmatizer configuration")
    } else {
      sender ! "processing"
      try {
        init()
        val map = new HashMap[String, Lemma]()
        val contentSize = content.length
        var done = 0
        sender ! done
        val sentences = MaxentTagger.tokenizeText(new StringReader(content))
        for (sentence <- sentences) {
          val tSentence = BookProcessActor.tagger.tagSentence(sentence)
          for (word <- tSentence) {
            if (word.word().size > 1 && word.word().matches("[A-Za-z]+")) {
              val tag = word.tag()
              if (BookProcessActor.tags.contains(tag)) {
                val lemmaStr = getLemma(word.word().toLowerCase(), tag).toLowerCase()
                val lemmaWord = BookProcessActor.tagger.tagSentence(MaxentTagger.tokenizeText(new StringReader(lemmaStr))
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
          done += sentence.length
          val p = (done / (contentSize / 100 + 1)).toInt
          logger.info("Parsed: " + p + " %")
          sender ! p
        }
        var i = 0;
        val wordsList = ListBuffer[BookWord]()
        logger.info("End parsing")
        map.foreach { case (key, value) => {
          //          WordnikService.getDictionaryEntry(value).onComplete(
          //            result =>
          //              if (result.isSuccess) {
          //                wordsList += result.get
          //              }
          //          )
          wordsList += BookWord(value.getWord.word(), value.getWord.tag(), value.getCount, ListBuffer[String](), ListBuffer[String](), ListBuffer[String]())
          i = i + 1
          sender ! (i / (map.size() / 100 + 1))
        }
        }


        //TODO extract writes
        implicit val writer = new Writes[BookWord] {
          def writes(word: (BookWord)): JsValue = {
            Json.obj(
              "word" -> word.word,
              "tag" -> word.tag,
              "freq" -> word.freq
            )
          }
        }

        sender ! Json.toJson(wordsList)
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

  private def init(): Unit = {
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
    ()
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