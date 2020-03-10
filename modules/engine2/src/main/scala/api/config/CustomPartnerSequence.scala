package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.ctx.{Site, Partner}
import xml.Elem
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * A PartnerSequence whose sequencing is passed-in. if `useProportionalAfterFirstCycle` is true, the
 * sequence will only be used once and then the `sequence` will be controlled by an instance of @see
 * ProportionalPartnerSequence. Otherwise, the sequence will cycle; the consequence of cycling is that
 * the passed-in `seq` will determine the rate at which a given Partner consumes their % of the queue,
 * e.g., if the `seq` is 100 partners long and Argentina is 50% of it, Argentina will have 50 opportunities
 * in the first 100 picks to consume it's (say) 2% of the queue.
 *
 */

class CustomPartnerSequence(val seq : List[Partner],
                            val site : Site,
                            val name : String = "Custom Partner Sequence",
                            val maybeUseAfterFirstCycle : Option[PartnerSequence] = None,
                            val maybePartnerWithInitialPick : Option[Partner] = None) extends PartnerSequence {
  private val LOGGER : Logger = LoggerFactory.getLogger(classOf[CustomPartnerSequence])
  def sequence: Stream[Partner] = {
   val initialPick = maybePartnerWithInitialPick.getOrElse(seq.head)
   val ps = filter(site, seq)
   val partnerStream = maybeUseAfterFirstCycle match {
     case None       => infiniteSequence(ps, 0)
     case Some(pSeq) => onceAndThen(ps, pSeq)
   }
   partnerStream.dropWhile(p => p != initialPick)
 }

 def configuration: Elem = <CustomPartnerSequence name={name} repeating={maybeUseAfterFirstCycle.isEmpty.toString}>
   { seq.map(_.toXML) }
 </CustomPartnerSequence>

 /* --------------------------- */
  private def filter(site: Site, seq : List[Partner]) = seq.filter(_.sites.contains(site))

  private def onceAndThen(seq : List[Partner], post : PartnerSequence) : Stream[Partner] = {
    seq.isEmpty match {
      case false => Stream.cons(seq.head, onceAndThen(seq.tail, post))
      case true => post.sequence
    }
  }

  // Creates a recursive definition of the stream, where the head is the
  // corresponding value and the tail is lazily evaluated when needed.
  private def infiniteSequence(s: List[Partner], i: Int): Stream[Partner] =  {
    if(i + 1 % 100 == 0){
      LOGGER.debug("PartnerSequence Cycle: " + i / 100)
      if((i + 1)% 100000 == 0){
        LOGGER.error("Queue Engine failed to terminate after 100,000 partner picks")
        val cycle = infiniteSequence(s, 100).take(100).toList.toString()
        throw new RuntimeException("Queue Engine failed to terminate after 100,000 partner picks. Using partner sequence: " + cycle)
      }
    }
    val partner = s(i % s.size)
    Stream.cons(partner, infiniteSequence(s, i + 1))
  }

}

object CsvToPartnerSequenceParser {

  type CsvParse = Either[Set[String], List[Partner]]

  def parse(csv : String, ps : List[Partner]) : CsvParse = {
    val tokens     = csv.split("[,/]").map(_.trim).toList
    val partnerMap = ps.map(p => p.id -> p).toMap
    val zero: Either[Set[String], List[Partner]] = Right(Nil)

    def lookupFailure(token: String, e: CsvParse): CsvParse =
      Left(e.left.getOrElse(Set.empty) + token)

    (tokens:\zero) { (t,e) =>
      partnerMap.get(t).fold(lookupFailure(t,e))(p => e.right.map(lst => p :: lst))
    }
  }
}