package edu.gemini.tac.qengine.impl.resource

import edu.gemini.tac.qengine.impl.block.Block
import edu.gemini.tac.qengine.api.config.BandRestriction
import edu.gemini.tac.qengine.api.queue.ProposalPosition
import edu.gemini.tac.qengine.log.{ProposalLog, RejectBand, RejectMessage}
import edu.gemini.tac.qengine.p1.{Proposal, QueueBand}
import edu.gemini.tac.qengine.util.Percent
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder

/**
 * Wraps BandRestrictions in a Resource interface.  The restrictions are not
 * changed over the course of the queue creation of course, so a successful
 * "reserve" just returns the same instance.
 */
final class BandResource(val lst: List[BandRestriction]) extends Resource {
  type T = BandResource

  def bandAndPercent(queue: ProposalQueueBuilder): (QueueBand, Percent) = {
    val perc = Percent((queue.usedTime.toHours.value / queue.queueTime.full.toHours.value * 100).round.toDouble)
    (queue.band, perc)
  }

  def rejectBand(prop: Proposal, name: String, queue: ProposalQueueBuilder): RejectBand = {
    val (band, percent) = bandAndPercent(queue)
    RejectBand(prop, name, band, percent)
  }

  /**
   * Checks the band restriction for the subset of restrictions for which the
   * proposal matches the predicate (is rapid TOO, requires LGS, etc).
   */
  private def checkBand(prop: Proposal, queue: ProposalQueueBuilder, valid: List[BandRestriction]): RejectMessage Either BandResource = {
    val band = queue.band
    valid.find(!_.bands.contains(band)) match {
      case None    => Right(this)
      case Some(r) => Left(rejectBand(prop, r.name, queue))
    }
  }


  private def checkBand(prop: Proposal, queue: ProposalQueueBuilder): RejectMessage Either BandResource =
    // Only consider restrictions for which the predicate matches this
    // proposal.
    lst.filter(_.matches(prop)) match {
      case Nil               => Right(this)
      case validRestrictions => checkBand(prop, queue, validRestrictions)
    }


  def reserve(block: Block, queue: ProposalQueueBuilder): RejectMessage Either BandResource =
    // This check can be rather expensive so we check only at the beginning and
    // end of a proposal.  Checking at the beginning is important because if it
    // will fail this test, it will likely fail at the beginning.  Failing
    // immediately will prevent a rollback.  Checking at the end is critical
    // because there we have better information about where the proposal will
    // actually fall in the queue.
    if (block.isStart || block.isFinal) checkBand(block.prop, queue) else Right(this)


  // --------------------------------------------------------------------------
  // ProposalQueue filtering to correct for moving proposals around during the
  // merge process.
  // --------------------------------------------------------------------------

  private def bandViolation(prop: Proposal, pos: ProposalPosition): Option[BandRestriction] =
    lst.filter(_.matches(prop)).find(!_.bands.contains(pos.band))

  private def isValid(prop: Proposal, pos: ProposalPosition): Boolean =
    bandViolation(prop, pos).isEmpty

  private def logMessage(prop: Proposal, pos: ProposalPosition, queue: ProposalQueueBuilder, r: BandRestriction): RejectBand = {
    val perc = Percent((pos.time.toHours.value / queue.queueTime.full.toHours.value * 100).round.toDouble)
    RejectBand(prop, r.name, pos.band, perc)
  }

  private def updatedLog(queue: ProposalQueueBuilder, log: ProposalLog, lst: List[(Proposal, ProposalPosition)]): ProposalLog =
    lst.foldLeft(log) {
      (curLog, tup) => {
        val prop = tup._1  // proposal that violates the restriction
        val pos  = tup._2  // position of the proposal relative orig queue

        val rest = bandViolation(prop, pos).get // which restriction violated
        val cat  = pos.band.logCategory         // Category for the log

        // Updated log.
        curLog.updated(prop.id, cat, logMessage(prop, pos, queue, rest))
      }
    }

  // This works assuming there aren't any band violations that work backwards,
  // like something valid in band 3 that isn't valid in band 1 or 2 ...
  // The % information in the log will be relative to the original queue.
  def filter(queue: ProposalQueueBuilder, log: ProposalLog): (ProposalQueueBuilder, ProposalLog) = {
    val newQueue = queue.positionFilter(isValid)
    val validIds = newQueue.toList.map(_.id).toSet
    val invalid  = queue.zipWithPosition.filterNot(tup => validIds.contains(tup._1.id))
    val newLog   = updatedLog(queue, log, invalid)
    (newQueue, newLog)
  }
}
