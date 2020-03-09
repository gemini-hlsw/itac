package edu.gemini.tac.qservice.impl.queue

import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.rollover.{RolloverSet => PsRolloverSet, IRolloverObservation => PsRolloverObservation}

import edu.gemini.tac.psconversion._

import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p2.rollover.{RolloverObservation, RolloverReport}
import edu.gemini.tac.qengine.p2.ObservationId

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/**
* Extracts rollover observation information from the persistence Queue.
*/
object RolloverExtractor {
  def UNSPECIFIED(x: String)             = "%s missing in rollover observation".format(x)
  def UNSPECIFIED_PARTNER                = UNSPECIFIED("Partner")
  def UNSPECIFIED_OBS_ID                 = UNSPECIFIED("Observation id")
  def BAD_OBS_ID(id: String)             = "Could not parse rollover observation id '%s'".format(id)
  def UNSPECIFIED_TARGET                 = UNSPECIFIED("Target")
  def UNSPECIFIED_CONDITIONS             = UNSPECIFIED("Observing conditions")
  def UNSPECIFIED_TIME                   = UNSPECIFIED("Time")
  def UNSPECIFIED_SITE                   = "Site information not included in queue"
  def UNSPECIFIED_OBSERVATIONS           = "Rollover report missing observations"

  def toRolloverObs(obs: PsRolloverObservation, partners: List[Partner], when: Long): PsError \/ RolloverObservation =
    for {
      psPartner <- nullSafePersistence(UNSPECIFIED_PARTNER) { obs.partner }
      partner   <- PartnerConverter.find(psPartner, partners)

      psObsId   <- nullSafePersistence(UNSPECIFIED_OBS_ID) { obs.observationId }
      obsId     <- ObservationId.parse(psObsId) \/> BadData(BAD_OBS_ID(psObsId))

      psTarget  <- nullSafePersistence(UNSPECIFIED_TARGET) { obs.target }
      target    <- TargetConverter.read(psTarget, when)

      psConds   <- nullSafePersistence(UNSPECIFIED_CONDITIONS) { obs.siteQuality }
      conds     <- ObsConditionsConverter.read(psConds)

      psTime    <- nullSafePersistence(UNSPECIFIED_TIME) { obs.observationTime }
      time      <- TimeConverter.toTime(psTime)
    } yield RolloverObservation(partner, obsId, target, conds, time)

  def toRolloverObsList(lst: List[PsRolloverObservation], partners: List[Partner], when: Long): PsError \/ List[RolloverObservation] =
    lst.map(toRolloverObs(_, partners, when)).sequenceU

  def toRolloverReport(rs: PsRolloverSet, partners: List[Partner], when: Long): PsError \/ RolloverReport =
    for {
      psSite <- nullSafePersistence(UNSPECIFIED_SITE) { rs.getSite }
      site   <- SiteConverter.read(psSite)
      psSet  <- nullSafePersistence(UNSPECIFIED_OBSERVATIONS) { rs.getObservations }
      lst    <- toRolloverObsList(psSet.asScala.toList, partners, when)
    } yield RolloverReport(lst).filter(site)

  /**
   * Extracts a rollover report from the persistence queue. Missing rollover
   * configuration generates an empty RolloverReport, not an error.
   */
  def extract(queue: PsQueue): PsError \/ RolloverReport =
    for {
      partners <- Partners.fromQueue(queue)
      ctx      <- ContextConverter.read(queue)
      rs       <- safePersistence { queue.getRolloverSet }
      rep      <- Option(rs).fold(RolloverReport.empty.right[PsError]) { rolloverSet =>
                    toRolloverReport(rolloverSet, partners.values, ctx.getMidpoint.getTime)
                  }
    } yield rep
}