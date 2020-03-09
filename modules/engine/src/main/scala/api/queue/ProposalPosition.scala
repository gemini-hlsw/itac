// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.queue

import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1.{Proposal, QueueBand}
import time.QueueTime

/**
 * Defines the position of a proposal in the queue in more detail relative to
 * other proposals.  Contains
 *
 * <ul>
 * <li>index - Index of the proposal in the queue.</li>
 * <li>time - Time taken by other proposals before this one in the queue.</li>
 * <li>band - Queue Band in which the proposal falls.</li>
 * <li>bandIndex - Index of the proposal within its queue band.</li>
 * <li>bandTime - Time taken by other proposals before this one relative to the
 * start of the queue band in which this proposal falls.</li>
 * </ul>
 */
case class ProposalPosition(
  index: Int,
  time: Time,
  band: QueueBand,
  bandIndex: Int,
  bandTime: Time
) {

  /**
   * Gets the position of the next proposal if the given proposal is the one
   * at the current position.  In other words, it advances the ProposalPosition
   * according to the time required by the given proposal.
   */
  def next(prop: Proposal, bandAt: Time => QueueBand): ProposalPosition =
    next(prop.time, bandAt(time + prop.time))

  def next(propTime: Time, nextBand: QueueBand): ProposalPosition = {
    val (i, t) = if (nextBand == band) (bandIndex + 1, bandTime + propTime) else (0, Time.Zero)
    ProposalPosition(index + 1, time + propTime, nextBand, i, t)
  }

  def programNumber: Int =
    band.number * 100 + bandIndex

}

object ProposalPosition {
  def apply(queueTime: QueueTime): ProposalPosition =
    new ProposalPosition(0, Time.ZeroHours, queueTime.band(Time.ZeroHours), 0, Time.ZeroHours)
}
