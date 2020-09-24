package edu.gemini.tac.qengine.api

import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.api.config.QueueEngineConfig
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p1.QueueBand

/**
 * The queue generation engine.
 */
trait QueueEngine {
  def calc(bandedProposals: Map[QueueBand, List[Proposal]], queueTime: QueueTime, config: QueueEngineConfig, partners: List[Partner], extras: List[Proposal] = Nil, removed: List[Proposal] = Nil): QueueCalc
}