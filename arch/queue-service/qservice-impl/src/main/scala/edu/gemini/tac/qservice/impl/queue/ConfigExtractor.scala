package edu.gemini.tac.qservice.impl.queue

import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.{Partner => PsPartner}
import edu.gemini.tac.psconversion._
import edu.gemini.tac.qengine.api.config.{PartnerSequence, QueueEngineConfig, SiteSemesterConfig, RestrictionConfig}
import edu.gemini.tac.qengine.api.queue.time.{PartnerTimeCalc, QueueTime}
import edu.gemini.tac.qengine.ctx.Site
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.p2.rollover.RolloverReport
import edu.gemini.tac.qservice.impl.queue.time.QueueTimeExtractor

import scalaz._
import Scalaz._


/**
 * Adapts the configuration information in the persistence Queue to the Queue
 * Engine.
 */
class ConfigExtractor(queue: PsQueue, props: List[Proposal] = Nil) {

  val partners = Partners.fromQueue(queue)

  /**
   * Extracts a RolloverReport from the persistence Queue, if possible.
   * A poorly configured rollover report produces a ConfigError.
   */
  val rolloverReport: PsError \/ RolloverReport =
    RolloverExtractor.extract(queue)


  private val qtimeEx: PsError \/ QueueTimeExtractor =
    for {
      rop <- rolloverReport
      parts <- partners
    } yield QueueTimeExtractor(queue, parts.values, rop, props)

  /**
   * Extracts QueueTime from the persistence Queue class, if possible.  A poorly
   * configured queue time produces a ConfigError.
   */
  val queueTime: PsError \/ QueueTime =
    for {
      qtx <- qtimeEx
      qt <- qtx.extract
    } yield qt

  /**
   * Extracts the partner time calculations that go into the QueueTime
   */
  val partnerTimeCalc: PsError \/ PartnerTimeCalc =
    for {
      qtx <- qtimeEx
      ptc <- qtx.partnerTimeCalc
    } yield ptc

  val sequence: PsError \/ PartnerSequence = {
    import PartnerSequenceConverter.{readCustom, readProportional}
    for {
      c     <- nullSafePersistence("Queue is missing committee") { queue.getCommittee }
      psSeq <- safePersistence(Option(c.getPartnerSequence))
      seq   <- psSeq.fold(readProportional(queue)) { readCustom(_,queue) }
    } yield seq
  }

  /**
   * Extracts the RestrictionConfig from the persistence Queue class, if
   * possible.  If a poorly configured band or time restriction is included in
   * the Queue, a ConfigError is generated.
   */
  val restrictionConfig: PsError \/ RestrictionConfig =
    for {
      tup <- TimeRestrictionExtractor.extract(queue)
      bandList <- BandRestrictionExtractor.extract(queue)
    } yield RestrictionConfig(tup._1, tup._2, bandList)

  /**
   * Extracts SiteSemesterConfig from the persistence Queue class, if possible.
   * Poorly configured bin options produce an error
   */
  val siteSemester: PsError \/ SiteSemesterConfig = SiteSemesterExtractor.extract(queue)

  /**
   * Pulls together the SiteSemesterConfig, RestrictionConfig, and Partner
   * sequence into the QueueEngineConfig, if possible.  Poorly configured
   * persistence Queue results in a ConfigError.
   */
  val config: PsError \/ QueueEngineConfig =
    for {
      prt     <- partners
      siteSem <- siteSemester
      seq     <- sequence
      rop     <- rolloverReport
      rest    <- restrictionConfig
    } yield QueueEngineConfig(prt.values, siteSem, seq, rop, rest)
}

object ConfigExtractor {
  private def name(ps: PsPartner): String = (for {
    abbr <- safePersistence { ps.getAbbreviation }
    n    <- safePersistence { ps.getName }
  } yield Option(abbr).orElse(Option(n)) | "<UNSPECIFIED>") | "<UNKNOWN>"

  def BAD_INITIAL_PARTNER_SITE(ps: PsPartner, site: Site): String = "Partner '%s' does not have time available at %s".format(name(ps), site.displayValue)

  def BAD_INITIAL_PARTNER(ps: PsPartner): String = "Partner '%s' not recognized".format(name(ps))

  def BAD_PARTNER_SEQUENCE(s : String) : String = "Partner sequence failed with exception '%s'".format(s)

  def apply(queue: PsQueue): ConfigExtractor = apply(queue, Nil)

  def apply(queue: PsQueue, props: List[Proposal]): ConfigExtractor = new ConfigExtractor(queue, props)
}
