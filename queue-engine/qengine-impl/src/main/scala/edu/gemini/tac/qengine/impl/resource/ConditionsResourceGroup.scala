package edu.gemini.tac.qengine.impl.resource

import edu.gemini.tac.qengine.api.config.{ConditionsBinGroup, ConditionsBin, ConditionsCategory => Cat}
import edu.gemini.tac.qengine.log._
import annotation.tailrec
import edu.gemini.tac.qengine.impl.block.Block
import edu.gemini.tac.qengine.util.{BoundedTime, Percent, Time}
import edu.gemini.tac.qengine.p1.ObsConditions
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import xml.Elem

object ConditionsResourceGroup {

  // Absorbs the time into the BoundedTime values associated with the given
  // "in" list of observing conditions categories, returning a list of updated
  // bins and any remaining time
  @tailrec private def reserveAvailable(t: Time, in: List[ConditionsBin[BoundedTime]], out: List[ConditionsBin[BoundedTime]]): (List[ConditionsBin[BoundedTime]], Time) =
    if (t.isZero || in.isEmpty)
      (out, t)
    else {
      val (bt, spill) = in.head.binValue.reserveAvailable(t)
      reserveAvailable(spill, in.tail, in.head.updated(bt) :: out)
    }

  /**
   * Constructs with the total time to spread across the observing conditions
   * bins and the set of bins to use.
   */
  def apply(t: Time, g: ConditionsBinGroup[Percent]) = {
    // Creates a map from ConditionsBin.Category to BoundedTime initialized with
    // time values according to the relative percentage of the matching
    // conditions bin.
    new ConditionsResourceGroup(g.map(perc => BoundedTime(t * perc)))
  }
}

/**
 * A time reservation used to keep up with the time used at the various
 * observing conditions categories.  Handles spilling the time across better
 * categories when necessary and possible.
 */
final class ConditionsResourceGroup private (val bins: ConditionsBinGroup[BoundedTime]) extends Resource {
  type T = ConditionsResourceGroup

  private def sum(c: ObsConditions, f: (BoundedTime => Time)): Time = {
    val cats = bins.searchPath(c)
    (Time.Minutes.zero/:cats)((t: Time, cat: Cat) => t + f(bins(cat)))
  }

  def limit(c: ObsConditions): Time = sum(c, _.limit)
  def remaining(c: ObsConditions): Time = sum(c, _.remaining)
  def isFull(c: ObsConditions): Boolean = remaining(c).isZero

  private def conds(block: Block, queue: ProposalQueueBuilder): ObsConditions =
    block.obs.conditions


  /**
   * Reserves the time for the given observation into the conditions bins
   * if possible.  Returns an updated ConditionsReservation containing the
   * observation's time, or else an error.
   */
  override def reserve(block: Block, queue: ProposalQueueBuilder): RejectMessage Either ConditionsResourceGroup = {
    val c = conds(block, queue)
    reserveAvailable(block.time, c) match {
      case (newGrp, t) if t.isZero => Right(newGrp)
      case _                       => Left(rejectConditions(block.prop, block.obs, queue.band, sum(c, _.used), sum(c, _.limit)))
    }
  }

  /**
   * Reserves up-to the given amount of time associated with the given set of
   * conditions.  If more time is specified than available, the bins associated
   * with these conditions will be filled and any remaining time will be
   * returned.   Returns a new conditions resource group with the reserved time
   * along with any remaining time that could not be reserved.
   */
  def reserveAvailable(time: Time, conds: ObsConditions): (ConditionsResourceGroup, Time) = {
    val (updatedBins, rem) = ConditionsResourceGroup.reserveAvailable(time, bins.searchBins(conds), Nil)
    (new ConditionsResourceGroup(bins.updated(updatedBins)), rem)
  }

  def toXML : Elem = <ConditionsResourceGroup>
    { bins.toXML }
    </ConditionsResourceGroup>

}
