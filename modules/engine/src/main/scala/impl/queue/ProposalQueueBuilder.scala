package edu.gemini.tac.qengine.impl.queue

import annotation.tailrec
import edu.gemini.tac.qengine.api.queue.ProposalPosition
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
object ProposalQueueBuilder {

  /**
   * Factory for ProposalQueue implementations.  QueueTime is required, but the
   * band percentages and merge strategy are optional.
   */
  def apply(queueTime: QueueTime): ProposalQueueBuilder =
    new ProposalQueueBuilder(queueTime)
}

final case class ProposalQueueBuilder(
  queueTime: QueueTime,
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


  /**
   * Gets the queue band at which the next proposal will be added.
   */
  val band: QueueBand = queueTime.band(usedTime)

  def toList: List[Proposal] = proposals.reverse

  /**
   * Gets the queue of proposals for each of the queue bands.
   */
  lazy val bandedQueue: Map[QueueBand, List[Proposal]] = {
    // Gets the map QueueBand -> List[Proposal] for all bands that are
    // actually present in the queue.
    val m = zipWithPosition.groupBy(tup => tup._2.band).mapValues(_.unzip._1)

    // Complete the map with empty lists for bands that weren't present.
    QueueBand.values.map(band => band -> m.getOrElse(band, Nil)).toMap
  }

  @tailrec
  private def zipWithPosition(pos: ProposalPosition, rem: List[Proposal], res: List[(Proposal, ProposalPosition)]): List[(Proposal, ProposalPosition)] =
    rem match {
      case Nil          =>
        res.reverse
      case head :: tail =>
        zipWithPosition(pos.next(head, queueTime.band _), tail, (head, pos) :: res)
    }

  def zipWithPosition: List[(Proposal, ProposalPosition)] =
    zipWithPosition(ProposalPosition(queueTime), toList, Nil)

  /**
   * Tail recursive implementation of positionOf.
   */
  @tailrec
  private def positionOf(prop: Proposal, pos: ProposalPosition, remaining: List[Proposal]): Option[ProposalPosition] =
    remaining match {
      case Nil          => None
      case head :: tail =>
        if (head.id == prop.id)
          Some(pos)
        else
          positionOf(prop, pos.next(head, queueTime.band _), tail)
    }

  def positionOf(prop: Proposal): Option[ProposalPosition] =
    positionOf(prop, ProposalPosition(queueTime), toList)


  private def extractIds(propList: List[Proposal]): Set[Proposal.Id] =
    propList.foldLeft(Set.empty[Proposal.Id]) {
      (set, prop) => set + prop.id
    }

  @tailrec
  private def calcRemoveSet(f: (Proposal, ProposalPosition) => Boolean, pos: ProposalPosition, remaining: List[Proposal], res: List[Proposal]): List[Proposal] =
    remaining match {
      case Nil          => res
      case head :: tail =>
        if (f(head, pos))
          // advance pos, don't remove head
          calcRemoveSet(f, pos.next(head, queueTime.band _), tail, res)
        else
          // don't advance pos since we are removing head
          calcRemoveSet(f, pos, tail, head :: res)
    }

  /**
   * Filters the proposal queue according to some criterion which may involve
   * the position of the proposal in the queue.  If the predicate function
   * rejects a proposal, the subsequent proposal obtains the position of the
   * rejected proposal.
   */
  def positionFilter(f: (Proposal, ProposalPosition) => Boolean): ProposalQueueBuilder =
    calcRemoveSet(f, ProposalPosition(queueTime), toList, Nil) match {
      case Nil => this  // Nothing removed, return this
      case lst => {
        val ids      = extractIds(lst)
        val filtered = proposals.filterNot(prop => ids.contains(prop.id))

        // Rebuild the queue with just the filtered proposals.
        new ProposalQueueBuilder(queueTime) ++ filtered.reverse
      }
    }


}