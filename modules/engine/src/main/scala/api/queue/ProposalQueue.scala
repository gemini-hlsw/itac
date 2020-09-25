package edu.gemini.tac.qengine.api.queue

import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.util.{BoundedTime, Time}
import edu.gemini.tac.qengine.ctx.Partner
import scalaz._, Scalaz._

/**
 * ProposalQueue is used to track the current state of the queue, including the
 * ordered proposals, the used time up to this point, the remaining time and the
 * queue band at which the next successful proposal will be added.  Once the
 * queue creation algorithm runs to completion, the bandedQueue method may be
 * used to extract the queue for each of the 3 queue bands
 */
trait ProposalQueue {

  /** Root queue time information. */
  def queueTime: QueueTime

  /** Gets the used time for the particular partner. */
  def usedTime(p: Partner): Time =
    toList.filter(_.ntac.partner == p).foldMap(_.time)

  /** Gets the amount of time used for a particular band. */
  def usedTime(band: QueueBand): Time = bandedQueue(band).foldMap(_.time)

  /**
   * Gets amount of time used by the proposals associated with the given
   * queue band for a particular partner.
   */
  def usedTime(band: QueueBand, p: Partner): Time  =
    bandedQueue(band).filter(_.ntac.partner == p).foldMap(_.time)

  /**
   * Gets the amount of time remaining (or unused) in the given queue band for
   * the given partner.
   */
  def remainingTime(band: QueueBand, partner: Partner): Time =
    queueTime(band, partner) - usedTime(band, partner)

  /**
   * Creates a BoundedTime object containing the total available queue time
   * and used time for the given band and partner.
   */
  def bounds(band: QueueBand, p: Partner): BoundedTime =
    BoundedTime(queueTime(band, p), usedTime(band, p))

  /** Gets the queue of proposals. */
  def toList: List[Proposal]

  /**
   * Gets the queue of proposals for each of the queue bands.
   */
  val bandedQueue: Map[QueueBand, List[Proposal]]

}