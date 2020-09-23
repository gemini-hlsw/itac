package edu.gemini.tac.qengine.api.queue

import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.QueueBand.Category
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.util.{BoundedTime, Time}
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.ProgramId

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


  /** Gets the amount of queue time used. */
  def usedTime: Time

  /** Gets the used time for the particular partner. */
  def usedTime(p: Partner): Time =
    Proposal.sumTimes(toList.filter(_.ntac.partner == p))

  /** Gets the amount of time used for a particular band. */
  def usedTime(band: QueueBand): Time = Proposal.sumTimes(bandedQueue(band))

  /** Gets the amount of time used for a particular category. */
  def usedTime(cat: Category): Time =
    Proposal.sumTimes(bandedQueue.filterKeys(_.categories.contains(cat)).values.flatten)

  /**
   * Gets amount of time used by the proposals associated with the given
   * queue band for a particular partner.
   */
  def usedTime(band: QueueBand, p: Partner): Time  =
    Proposal.sumTimes(bandedQueue(band).filter(_.ntac.partner == p))

  /**
   * Gets amount of time used by the proposals associated with the given
   * queue band for a particular category and partner.
   */
  def usedTime(cat: Category, p: Partner): Time = {
    val props = bandedQueue.filterKeys(_.categories.contains(cat)).values.flatten.toList
    Proposal.sumTimes(props.filter(_.ntac.partner == p))
  }


  /** Gets the amount of  queue time that is remaining. */
  def remainingTime: Time = queueTime.full - usedTime

  /** Gets the amount of remaining queue time for the specified partner. */
  def remainingTime(p: Partner): Time = queueTime(p) - usedTime(p)

  /**
   * Gets the amount of time remaining (or unused) in the given queue band.
   * Will be negative if the band was over allocated.  Band 1 may "steal" time
   * from Band 2, which in turn may steal time from Band 3, which may steal
   * time from Band 4.  If a queue band is overallocated, the remaining time
   * will be negative.
   */
  def remainingTime(band: QueueBand): Time =
    queueTime(band) - usedTime(band)

  /**
   * Gets the amount of time remaining (or unused) in the given queue band for
   * the given partner.
   */
  def remainingTime(band: QueueBand, partner: Partner): Time =
    queueTime(band, partner) - usedTime(band, partner)

  /** Gets the amount of remaining queue time for the given category. */
  def remainingTime(c: Category): Time = queueTime(c) - usedTime(c)

  /**
   * Gets the amount of time remaining (or unused) in the given queue band
   * category for the given partner.
   */
  def remainingTime(c: Category, p: Partner): Time =
    queueTime(c, p) - usedTime(c, p)


  /**
   * Creates a BoundedTime object containing the total available queue time
   * for all partners and bands together along with the amount of used time
   * for all partners and bands.
   */
  def bounds = BoundedTime(queueTime.full, usedTime)

  /**
   * Creates a BoundedTime object containing the total available queue time
   * and used time for the given partner.
   */
  def bounds(p: Partner) = BoundedTime(queueTime(p), usedTime(p))

  /**
   * Creates a BoundedTime object containing the total available queue time
   * and used time for the given band.
   */
  def bounds(band: QueueBand): BoundedTime =
    BoundedTime(queueTime(band), usedTime(band))

  def bounds(cat: QueueBand.Category): BoundedTime =
    BoundedTime(queueTime(cat), usedTime(cat))

  /**
   * Creates a BoundedTime object containing the total available queue time
   * and used time for the given band and partner..
   */
  def bounds(band: QueueBand, p: Partner): BoundedTime =
    BoundedTime(queueTime(band, p), usedTime(band, p))

  def bounds(cat: QueueBand.Category, p: Partner): BoundedTime =
    BoundedTime(queueTime(cat, p), usedTime(cat, p))

  /**
   *  Gets the queue of proposals.  Joint parts will be merged in the list
   * that is returned.
   */
  def toList: List[Proposal]

  /**
   * Gets the queue of proposals for each of the queue bands.
   */
  val bandedQueue: Map[QueueBand, List[Proposal]]

}