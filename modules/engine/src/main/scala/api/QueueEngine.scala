package edu.gemini.tac.qengine.api

import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.api.config.QueueEngineConfig
import edu.gemini.tac.qengine.p1.QueueBand

/**
 * The queue generation engine.
 */
trait QueueEngine {
  def calc(
    bandedProposals: QueueBand => List[Proposal],
    queueTimes:   QueueBand => QueueTime,
    config: QueueEngineConfig,
    extras: List[Proposal] = Nil,
    removed: List[Proposal] = Nil
  ): QueueCalc
}