package edu.gemini.tac.qservice.impl.persist

import edu.gemini.tac.persistence.{Proposal => PsProposal}
import edu.gemini.tac.persistence.queues.{Banding => PsBanding}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.qengine.api.queue.ProposalQueue
import edu.gemini.tac.qengine.ctx.Context
import edu.gemini.tac.qengine.p1.{Proposal, QueueBand}

import collection.JavaConverters._

/**
* Persists banding objects detailing decisions made by the queue algorithm.
*/
class BandingPersister(val ctx: Context, val pool: List[(PsProposal, Proposal)]) {

  def persist(propQueue: ProposalQueue, queue: PsQueue) {
    val bandMap  = PreBanding.toList(ctx, propQueue).groupBy { _.pos.band }
    val bandings = bandMap.toList.flatMap {
      case (band, lst) => toBanding(band, queue, lst)
    }
    queue.setBandings(new java.util.TreeSet[PsBanding](bandings.asJava))
  }

  private def toBanding(band: QueueBand, queue: PsQueue, lst: List[PreBanding]): List[PsBanding] = {
    val fact = new PsBandingFactory(band, pool, queue)
    lst.map { pre => fact(pre.prop, pre.pos.index, pre.progId) }
  }
}