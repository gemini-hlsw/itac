package edu.gemini.tac.qengine.impl.queue

import edu.gemini.tac.qengine.p1.{QueueBand, Proposal}
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.api.queue.ProposalQueue
import scalaz._, Scalaz._

/**
 * A ProposalQueue implementation that contains a final queue calculation.
 */
class FinalProposalQueue(val queueTime: QueueTime, bandMap: Map[QueueBand, List[Proposal]]) extends ProposalQueue {

  // Complete the map with empty lists for bands that weren't present.
  val bandedQueue: Map[QueueBand, List[Proposal]] =
    QueueBand.values.map(band => band -> bandMap.getOrElse(band, Nil)).toMap

  // behavior changes if we move this up to ProposalQueue, why?
  val usedTime: Time =
    toList.foldMap(_.time)

  def toList: List[Proposal] =
    QueueBand.values.flatMap(bandedQueue)

}