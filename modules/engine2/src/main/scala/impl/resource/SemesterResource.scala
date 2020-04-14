package edu.gemini.tac.qengine.impl.resource

import edu.gemini.tac.qengine.impl.block.Block
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.log.{RejectOverAllocation, RejectMessage, RejectPartnerOverAllocation}
import edu.gemini.tac.qengine.p1.QueueBand.Category.Guaranteed
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import edu.gemini.tac.qengine.p1.{ObservingConditions, Target}
import org.slf4j.LoggerFactory
import edu.gemini.tac.qengine.p1.QueueBand

final case class SemesterResource(
    ra:   RaResourceGroup,
    time: TimeResourceGroup,
    band: BandResource,
    cat:  QueueBand.Category
) extends Resource {
  private val LOGGER = LoggerFactory.getLogger("edu.gemini.itac")
  type T = SemesterResource

  private def reserveAll(block: Block, queue: ProposalQueueBuilder): RejectMessage Either SemesterResource =
    for {
      newRa   <- ra.reserve(block, queue).right
      newTime <- time.reserve(block, queue).right
      newBand <- band.reserve(block, queue).right
    } yield new SemesterResource(newRa, newTime, newBand, cat)

  // Determines whether the partner is already over allocated.
  private def partnerAlreadyOverallocated(block: Block, queue: ProposalQueueBuilder): Boolean = {
    LOGGER.debug(f"Remaining time in $cat for ${block.prop.ntac.partner} is ${queue.remainingTime(cat, block.prop.ntac.partner).toHours.value}%5.1f")
    queue.remainingTime(cat, block.prop.ntac.partner) <= Time.Zero
  }

  // Determines whether including the indicated proposal will overallocate the
  // partner past the limit and allowance.
  private def partnerWouldBeOverallocated(block: Block, queue: ProposalQueueBuilder): Boolean =
    queue.queueTime.partnerOverfillAllowance exists { perc =>
      val partner   = block.prop.ntac.partner
      val used      = queue.usedTime(cat, partner)
      val allow     = queue.queueTime.fullPartnerTime(partner) * perc
      val hardlimit = queue.queueTime(cat, partner) + allow
      (used + block.prop.time) >= hardlimit
    }

  private def partnerOverallocated(block: Block, queue: ProposalQueueBuilder): Boolean =
    partnerAlreadyOverallocated(block, queue) || partnerWouldBeOverallocated(block, queue)

  // Determines whether the queue has space to accommodate the proposal.
  // queue.remainingTime tells us how much scheduable time is remaining.  We
  // are allowed to schedule the last proposal past this time but never more
  // that the total queue time.  In other words, the hard limit is the
  // full queue time.
  private def queueTooFull(block: Block, queue: ProposalQueueBuilder): Boolean =
    (queue.remainingTime(cat) <= Time.Zero) || queue.remainingTime < block.prop.time

  def reserve(block: Block, queue: ProposalQueueBuilder): RejectMessage Either SemesterResource = {
    // Check that we haven't over allocated this partner.  If so, rejected.
    // Otherwise, try to reserve the time.

    // We only need to check for overallocation at the start of the block --
    // the queue.remainingTime for the partner won't be updated until another
    // proposal is added so there is no need to check with every block that is
    // considered.
    if (block.isStart && partnerOverallocated(block, queue)) {
      val p = block.prop.ntac.partner
      LOGGER.debug("Rejected due to partner overallocation")
      Left(RejectPartnerOverAllocation(block.prop, queue.bounds(cat, p), queue.bounds(p)))
    } else if (queueTooFull(block, queue)) {
      LOGGER.debug("Rejected due to queue too full")
      Left(RejectOverAllocation(block.prop, queue.remainingTime(cat), queue.remainingTime))
    }
    else {
      LOGGER.debug("Block OK")
      if(block.isFinal){
        LOGGER.debug("Block is final; proposal will be accepted")
      }
      reserveAll(block, queue)
    }
  }

  def reserveAvailable(time: Time, target: Target, conds: ObservingConditions): (SemesterResource, Time) = {
    val (newRa, rem) = ra.reserveAvailable(time, target, conds)
    (new SemesterResource(newRa, this.time, band, cat), rem)
  }

  def toXML = <SemesterResource>
    { ra.toXML }
    { time.toXML }
    { band.toXML }
    </SemesterResource>

}