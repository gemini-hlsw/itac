package edu.gemini.tac.qengine.impl.queue

import edu.gemini.tac.qengine.api.queue.ProposalQueue
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.QueueBand.{QBand3, QBand4}
import scalaz._, Scalaz._

/**
 * ProposalQueueBuilder is used to construct the Band 1, 2, and 3 part of the
 * proposal queue.  In other words, for the part of the queue that is determined
 * by the relative time occupied by previous proposals in the queue.  It tracks
 * the current state of the queue computation including the ordered proposals,
 * time used up to the current point, and the queue band.  Once the queue
 * creation algorithm runs to completion the bandedQueue method may be used to
 * extract the queue for each of the 3 queue bands.
 */

final case class ProposalQueueBuilder(
  queueTime: QueueTime,
  band: QueueBand,
  proposals: List[Proposal] = Nil
) extends ProposalQueue {

  lazy val usedTime = proposals.foldMap(_.time)

  /**
   * Adds the given proposal to the queue and returns a new ProposalQueue
   * reflecting the change.
   *
   * @throws IllegalArgumentException if in band 3 and the proposal cannot be
   * scheduled in band3, or if the proposal would use more than the remaining
   * queue time
   */
  def :+(prop: Proposal): ProposalQueueBuilder = {
    val curBand = band

    require((curBand == QBand4) || ((usedTime + prop.time) <= queueTime.full),
      "Cannot schedule guaranteed time past the total available queue time.")

    // See ITAC-415, ITAC-416.  If we do not filter out band 3 during band
    // restriction tests, we cannot have this requirement.
    require((curBand != QBand3) || prop.band3Observations.size != 0,
      "Proposal cannot be scheduled in band 3.")

    copy(proposals = prop :: proposals)
  }

  /**
   * Adds all the proposals to the queue in the traversal order.
   */
  def ++(props: Traversable[Proposal]): ProposalQueueBuilder =
    props.foldLeft(this) {
      (q, p) =>
//        println("%d - %s - %.1f".format(q.band.number, p.id, q.usedTime.toHours.value))
        q :+ p
    }


  def toList: List[Proposal] = proposals.reverse

  val bandedQueue: Map[QueueBand,List[Proposal]] =
    QueueBand.values.foldMap(b => Map(b -> List.empty[Proposal])) ++
    Map(band -> proposals)

}