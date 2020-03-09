package edu.gemini.tac.qservice.impl.queue.time

import edu.gemini.tac.persistence.{Partner => PsPartner}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.queues.partnerCharges.{PartnerCharge => PsPartnerCharge}
import edu.gemini.tac.psconversion._

import edu.gemini.tac.qengine.api.config.QueueBandPercentages
import edu.gemini.tac.qengine.api.queue.time.{PartnerTime, PartnerTimeCalc, QueueTime}
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.p2.rollover.RolloverReport
import edu.gemini.tac.qengine.util.{Time, Percent}

import scala.annotation.tailrec

import scalaz._
import Scalaz._
import edu.gemini.tac.qengine.ctx.Partner

object QueueTimeExtractor {
  def BAD_BAND1_CUTOFF     = "Missing or unparseable Band 1 cutoff"
  def BAD_BAND2_CUTOFF     = "Missing or unparseable Band 2 cutoff"
  def BAD_BAND3_CUTOFF     = "Missing or unparseable Band 3 cutoff"
  def BAD_BAND_PERCENTAGES(b1: Int, b2: Int, b3: Int) = "Band cutoffs must be in increasing order, and all must be in range 0 - 100 (not %d, %d, %d)".format(b1, b2, b3)

  def BAD_PARTNER_TIME     = "Missing or bad partner time"

  def BAD_ROLLOVER_PARTNER = "Missing or unrecognized rollover time partner"
  def BAD_ROLLOVER_TIME    = "Missing or unparseable rollover time amount"

  def NEGATIVE_LPT_TIME(timeVal: Int) = "Large program time must be a positive number, not %d".format(timeVal)

  def TOO_MUCH_OVERFILL(x: Int)        = s"Band 3 cutoff plus overfill factor ($x%) cannot be more than 100%"
  def NEGATIVE_OVERFILL_FACTOR(x: Int) = s"Overfill factor must be a positive number, not $x"

  /**
   * Turns a List of Pairs of Option into an Either List of Pairs.  If any
   * element of any Pair in the input is None, then the entire result is None
   */
  @tailrec
  private def toRight[T,U](rem: List[(Option[T], Option[U])], res: List[(T, U)], badT: String, badU: String): PsError \/ List[(T, U)] =
    rem match {
      case Nil                        => res.reverse.right[PsError]
      case (Some(t), Some(u)) :: tail => toRight(tail, (t, u) :: res, badT, badU)
      case (None,    _)       :: tail => BadData(badT).left[List[(T,U)]]
      case (_,       None)    :: tail => BadData(badU).left[List[(T,U)]]
    }

  def apply(psQueue: PsQueue, partners: List[Partner], rop: RolloverReport, propList: List[Proposal] = Nil): QueueTimeExtractor =
    new QueueTimeExtractor(psQueue, partners, rop, propList)
}

import QueueTimeExtractor._

/**
* Extracts QueueEngine QueueTime from the configuration information in a
* persistence Queue.
*/
class QueueTimeExtractor(queue: PsQueue, partners: List[Partner], rop: RolloverReport = RolloverReport.empty, props: List[Proposal] = Nil) {
  // TODO
  // * Not using "useBand3AfterThresholdCrossed" flag yet -- always using band 3

  private def bandPercentages(b1: Int, b2: Int, b3: Int): PsError \/ QueueBandPercentages =
    if ((b1 < 0) || (b2 < b1) || (b3 < b2) || (b3 > 100)) BadData(BAD_BAND_PERCENTAGES(b1, b2, b3)).left
    else QueueBandPercentages(b1, b2-b1, b3-b2).right

  private val parsedCtx = ContextConverter.read(queue)

  /**
   * Gets the queue band percentages to use, if possible.
   */
  val bandPercentages: PsError \/ QueueBandPercentages =
    for {
      b1 <- safePersistenceInt(BAD_BAND1_CUTOFF) { queue.getBand1Cutoff }
      b2 <- safePersistenceInt(BAD_BAND2_CUTOFF) { queue.getBand2Cutoff }
      b3 <- safePersistenceInt(BAD_BAND3_CUTOFF) { queue.getBand3Cutoff }
      p  <- bandPercentages(b1, b2, b3)
    } yield p

  /**
   * Extracts the overfill factor.  If not specified, 0 is assumed.  If
   * specified and negative, it is an error.
   */
  val overfillFactor: PsError \/ Percent = {
    val over = safePersistenceInt(0) { queue.getOverfillLimit }
    over.flatMap { i =>
      if (i < 0) BadData(NEGATIVE_OVERFILL_FACTOR(i)).left
      else Percent(i).right
    }
  }

  // TODO
  // * Partner Quanta is computed and differs slightly for each Partner.  I
  // think they want to be able to fiddle with the value for total time merged
  // per partner sequence cycle. That's totally doable but currently hardcoded
  // to their suggested default of 300 hours.

  /**
   * Gets the nominal partner time based solely on partner percentages and the
   * specified total queue time with no adjustments made.
   *
   * @return ConfigError if the total queue time is null or negative, a
   * PartnerTime otherwise
   */
  val basePartnerTime: PsError \/ PartnerTime = {
    def available: PsError \/ Time =
      safePersistenceInt(BAD_PARTNER_TIME) { queue.getTotalTimeAvailable }.flatMap { hrs =>
        if (hrs <= 0) BadData(BAD_PARTNER_TIME).left
        else Time.hours(hrs).right
      }

    for {
      ctx <- parsedCtx
      pt  <- available.map { hrs => PartnerTimeCalc.base(ctx.getSite, hrs, partners)}
    } yield pt
  }

  /**
   * Computes the time allocated to classical programs for the site per partner.
   */
  val classical: PsError \/ PartnerTime =
    for {
      ctx <- parsedCtx
    } yield PartnerTimeCalc.classical(ctx.getSite, props, partners)

  /**
   * Extracts the rollover time into a Map[Partner, Time], if possible.
   * Rollover time is distributed evenly over the partners.  In other words,
   * we reduce the queue time in general to account for rollover time and don't
   * map it to specified partners for which rollover observations exist.
   *
   * <p>As stated in the 11A Software Requirements (v5, 11A-TAC-19):
   * All observations containing non-zero planned time that are contained in
   * the report will be charged against the total queue time. Partners are not
   * individually deducted time for rollover proposals.</p>
   *
   * <p>For this reason, the time is distributed across partners evenly by this
   * calculation.</p>
   */
  val rollover: PsError \/ PartnerTime =
    for {
      ctx <- parsedCtx
    } yield PartnerTimeCalc.rollover(ctx.getSite, rop, partners)


  private def pcConverter[T <: PsPartnerCharge](name: String, f: PsQueue => java.util.Map[PsPartner, T]): PsError \/ PartnerTime =
    for {
      pc <- nullSafePersistence(s"$name partner charges missing") { f(queue) }
      pt <- new PartnerChargeConverter(name, pc, partners).partnerTime
    } yield pt

  /**
   * Extracts exchange program time configuration.  Exchange time is handed in
   * to the queue service.  No calculation is necessary, we just need to
   * extract it into a PartnerTime and apply it to the total queue time
   * available.
   */
  val exchangeTime: PsError \/ PartnerTime = pcConverter("Exchange", _.getExchangePartnerCharges)
  val adjustments: PsError \/ PartnerTime = pcConverter("Adjustment", _.getAdjustmentPartnerCharges)
  val partnerTrade: PsError \/ PartnerTime = pcConverter("Partner Exchange", _.getPartnerExchangePartnerCharges)

  /**
   * Extracts categorized PartnerTimes from the configuration.
   */
  val partnerTimeCalc: PsError \/ PartnerTimeCalc =
    for {
      b     <- basePartnerTime
      c     <- classical
      rol   <- rollover
      exc   <- exchangeTime
      adj   <- adjustments
      trade <- partnerTrade
    } yield new PartnerTimeCalc(partners, b, classical = c, rollover = rol, exchange = exc, adjustment = adj, partnerTrade = trade)

  /**
   * Computes QueueTime from the definition in Queue, if possible.  Adjusts for
   * rollover time.
   */
  def extract: PsError \/ QueueTime =
    for {
      ctx <- parsedCtx
      ptc <- partnerTimeCalc
      p   <- bandPercentages
      of  <- overfillFactor
    } yield QueueTime(ctx.getSite, ptc.net, p, Some(of))
}

