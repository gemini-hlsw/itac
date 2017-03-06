package edu.gemini.tac.qservice.impl.queue

import edu.gemini.tac.persistence.queues.Queue
import edu.gemini.tac.persistence.bandrestriction.{BandRestrictionRule => PsBandRestriction}
import edu.gemini.tac.persistence.bandrestriction.{Iq20RestrictionNotInBand3 => PsIQ20BandRestriction}
import edu.gemini.tac.persistence.bandrestriction.{LgsRestrictionInBandsOneAndTwo => PsLgsBandRestriction}
import edu.gemini.tac.persistence.bandrestriction.{RapidTooRestrictionInBandOne => PsRapidTooBandRestriction}
import edu.gemini.tac.persistence.bandrestriction.{LargeProgramRestrictionNotInBand3 => PsLargeProgramRestrictionNotInBand3}
import edu.gemini.tac.psconversion._

import edu.gemini.tac.qengine.api.config.BandRestriction
import edu.gemini.tac.qengine.util.QEngineUtil._

import scalaz._
import Scalaz._

/**
 * Extracts band restrictions from the persistence Queue object
 */
object BandRestrictionExtractor {
  private def name(ps: PsBandRestriction): String = {
    val o = safePersistence { ps.getName }.fold(_ => none[String], s => Option(s))
    o | ps.getClass.getName
  }

  def UNRECOGNIZED_BAND_RESTRICTION(ps: PsBandRestriction) =
     "Unrecognized band restriction: %s".format(name(ps))

  // This is a bit extreme, but we're only using the PsBandRestriction
  // subclasses to pick out the matching BandRestriction configuration.  Not
  // clear that there should be PsBandRestriction classes.
  private def toBandRestriction(ps: PsBandRestriction): PsError \/ BandRestriction =
    ps match {
      case _: PsIQ20BandRestriction               => BandRestriction.iq20.right
      case _: PsLgsBandRestriction                => BandRestriction.lgs.right
      case _: PsRapidTooBandRestriction           => BandRestriction.rapidToo.right
      case _: PsLargeProgramRestrictionNotInBand3 => BandRestriction.largeProgram.right
      case _ => BadData(UNRECOGNIZED_BAND_RESTRICTION(ps)).left
    }

  private def bandRestrictions(lst: List[PsBandRestriction]): PsError \/ List[BandRestriction] =
    lst.map(toBandRestriction).sequenceU.map {
      restrictionList => BandRestriction.notBand3 :: restrictionList
    }

  def extract(queue: Queue): PsError \/ List[BandRestriction] =
    for {
      br  <- safePersistence { queue.getBandRestrictionRules }
      res <- bandRestrictions(toList(br))
    } yield res
}