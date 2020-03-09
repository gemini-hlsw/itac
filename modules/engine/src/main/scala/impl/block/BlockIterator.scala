// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.impl.block

import edu.gemini.tac.qengine.util.Time

import BlockIterator.IMap
import edu.gemini.tac.qengine.api.queue.time.PartnerTimes
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p1.{Observation, Proposal}
import org.slf4j.LoggerFactory

/**
 * An immutable iterator that can be used to generate time blocks across all
 * partners and proposals.  It combines single partner iterators, slicing the
 * time across partners according to a provide sequence and map of partner
 * time quanta.
 */
trait BlockIterator {

  val allPartners: List[Partner]

  /**
   * Maps a Partner to the length of its time quantum.
   */
  val quantaMap: PartnerTimes

  /**
   * The remaining sequence of Partner time quanta.  As the iterator progresses,
   * the sequence advances.
   */
  val seq: Seq[Partner]

  /**
   * Map of Partner to PartnerTimeBlockIterator.  As the iterator progresses,
   * the PartnerTimeBlockIterators are advanced.
   */
  val iterMap: IMap

  /**
   * The time remaining in the current time quantum.
   */
  val remTime: Time

  /**
   * Computes the list of remaining proposals in the iterator.
   */
  def remPropList: List[Proposal] =
    allPartners.flatMap(p => iterMap(p).remainingProposals)

  /**
   *  The partner that occupies the current time quantum.
   */
  def currentPartner: Partner = seq.head

  def isStartOf(prop: Proposal): Boolean =
    (currentPartner == prop.ntac.partner) &&
      iterMap(currentPartner).isStartOf(prop)

  /**
   * Whether this iterator will produce any time blocks.  This method should
   * be called before calling any other methods.  If it returns
   * <code>false</code>, the result of using the other methods is not
   * specified.
   */
  def hasNext: Boolean = iterMap(currentPartner).hasNext

  /**
   * Generates the next TimeBlock and a new iterator configured to produce the
   * remaining blocks.
   */
  final def next(activeList: Proposal => List[Observation]): (Block, BlockIterator) = {

    // Advance the partner time block iterator.  Use at most remTime time.
    // This may cause the iterator to generate a block for part of an
    // observation, which is fine.
    val curr          = iterMap(currentPartner)
    val (block, iter) = curr.next(remTime, activeList)

    LoggerFactory
      .getLogger(this.getClass)
      .debug(s"    👉  Current partner is ${Console.BOLD}$currentPartner${Console.RESET}.")
    LoggerFactory.getLogger(this.getClass).debug(s"         current: $curr")
    // LoggerFactory.getLogger(this.getClass).debug(s"       next:    $iter")
    LoggerFactory.getLogger(this.getClass).debug(s"         block:   $block")

    // Return the block and advance this iterator.  This may move to another
    // observation in the same time quantum or, if we've reached the end of the
    // quantum, it will advance to the next partner in the sequence.  Use the
    // updated map of partner time block iterators.
    val updated  = iterMap.updated(currentPartner, iter)
    val nextIter = advance(block.time, updated)

    // if (!nextIter.hasNext)
    // LoggerFactory.getLogger(this.getClass).debug(s"  👉  This will be the last iterator.")

    (block, nextIter)
  }

  /**
   * Extracts all the TimeBlocks from this iterator into a single list.
   * This method is mostly intended for testing support since it is not
   * tail recursive and could be expensive for lengthy sequences.
   */
  def toList(activeList: Proposal => List[Observation]): List[Block] =
    if (!hasNext) Nil
    else
      next(activeList) match {
        case (b, it) => b :: it.toList(activeList)
      }

  /**
   * Skips the proposal that would be generated in the next TimeBlock.
   */
  def skip(activeList: Proposal => List[Observation]): BlockIterator = {
    val partnerIter = iterMap(currentPartner).skip(activeList)
    val m           = iterMap.updated(currentPartner, partnerIter)
    if (partnerIter.hasNext) mkIterator(seq, remTime, m) else advancePartner(m)
  }

  private def advance(t: Time, m: IMap): BlockIterator = {
    LoggerFactory
      .getLogger(this.getClass)
      .debug(
        s"    👉  Pre-compute the next state. There are ${remTime - t} remaining in this quantum. There are${if (m(currentPartner).hasNext) ""
        else "no"} remaining proposal(s) for ${currentPartner.id}."
      )
    if ((remTime > t) && m(currentPartner).hasNext) {
      LoggerFactory.getLogger(this.getClass).debug(s"      👉  Next proposal for $currentPartner.")
      mkIterator(seq, remTime - t, m)
    } else {
      val it = advancePartner(m)
      LoggerFactory.getLogger(this.getClass).debug(s"      👉  Advancing to next partner.")
      it
    }
  }

  private def advancePartner(m: IMap): BlockIterator =
    advancePartner(seq.tail, m)

  private def advancePartner(
    s: Seq[Partner],
    blockIteratorByPartner: IMap,
    remaining: Set[Partner] = BlockIterator.validpartners(allPartners, quantaMap)
  ): BlockIterator = {
    if (remaining.isEmpty || s.isEmpty) {
      LoggerFactory.getLogger(this.getClass).debug(s"        👉  remaining=$remaining seq=$s")

      //QueueCalculationLog.logger.log(Level.INFO, "BlockIterator.empty()")
      // LOGGER.debug(<Event source="BlockIterator" event="Empty"/>.toString())
      new BlockIterator.Empty(allPartners)
    } else {
      val hasNext1      = blockIteratorByPartner(s.head).hasNext
      val hasQuantaTime = !quantaMap(s.head).isZero
      if (hasNext1 && hasQuantaTime) {
        mkIterator(s, quantaMap(s.head), blockIteratorByPartner)
      } else {
        val moreSeq = s.tail
        // moreSeq.isEmpty match {
        //   case true => // LOGGER.debug("End of sequence")
        //   case false => {
        //     val nextPartner = moreSeq.head
        //     // LOGGER.debug(<Event source="BlockIterator" event="advancePartner">
        //       // {nextPartner.fullName}
        //     // </Event>.toString)
        //   }
        // }
        //QueueCalculationLog.logger.log(Level.INFO, (<Event source="BlockIterator" event="advancePartner">{s.head.fullName}</Event>).toString)
        advancePartner(moreSeq, blockIteratorByPartner, remaining - s.head)
      }
    }
  }

  protected def mkIterator(partnerSeq: Seq[Partner], t: Time, iterMap: IMap): BlockIterator
}

object BlockIterator {
  type IMap = Map[Partner, PartnerBlockIterator]

  private case class Empty(val allPartners: List[Partner]) extends BlockIterator {
    val quantaMap: PartnerTimes = PartnerTimes.empty
    val seq: Seq[Partner]      = Seq.empty
    val remTime: Time          = Time.Zero
    val iterMap: IMap          = Map.empty

    override def isStartOf(prop: Proposal): Boolean   = false
    override def remPropList: List[Proposal]          = Nil
    override def hasNext: Boolean                     = false
    def mkIterator(s: Seq[Partner], t: Time, m: IMap) = this
  }

  private case class BlockIteratorImpl(
    val allPartners: List[Partner],
    val quantaMap: PartnerTimes,
    val seq: Seq[Partner],
    val remTime: Time,
    val iterMap: IMap
  ) extends BlockIterator {

    def mkIterator(s: Seq[Partner], t: Time, m: IMap) = {
      //QueueCalculationLog.logger.log(Level.INFO, (<Event source="BlockIterator" event="mkIterator">{s.head.fullName}</Event>).toString)
      new BlockIteratorImpl(allPartners, quantaMap, s, t, m)
    }
  }

  private def genIterMap(
    allPartners: List[Partner],
    m: Map[Partner, List[Proposal]],
    activeList: Proposal => List[Observation]
  ): IMap =
    Partner.mkMap(allPartners, m, Nil).mapValues(PartnerBlockIterator.apply(_, activeList))

  // Finds the first partner that has a non-zero time quantum and a proposal
  // list and returns the sequence advanced to that partner and the time in its
  // time quantum.
  private def init(
    qMap: PartnerTimes,
    iMap: IMap,
    partnerSeq: Seq[Partner],
    remaining: Set[Partner]
  ): (Seq[Partner], Time) =
    if (remaining.isEmpty || partnerSeq.isEmpty)
      (Seq.empty, Time.Zero)
    else if (!qMap(partnerSeq.head).isZero && iMap(partnerSeq.head).hasNext)
      (partnerSeq, qMap(partnerSeq.head))
    else
      init(qMap, iMap, partnerSeq.tail, remaining - partnerSeq.head)

  // Calculates an initial set of valid partners.  It trims any partners
  // without a time quanta.  These partners should not appear in the sequence.
  private def validpartners(allPartners: List[Partner], quantaMap: PartnerTimes): Set[Partner] =
    allPartners.filter(!quantaMap(_).isZero).toSet

  /**
   * Constructs the TimeBlockIterator for the appropriate queue band category,
   * using the time quanta indicated in the quantaMap, the sorted proposals in
   * the propLists, and the provided sequence of partners.
   *
   * <p>The partner sequence can be finite but an infinite sequence is expected
   * in order to be able to generate time blocks for all the proposals.
   */
  def apply(
    allPartners: List[Partner],
    quantaMap: PartnerTimes,
    seq: Seq[Partner],
    propLists: Map[Partner, List[Proposal]],
    activeList: Proposal => List[Observation]
  ): BlockIterator = {
    val iterMap = genIterMap(allPartners, propLists, activeList)

    // HMM: `filterNot` below causes a test failure
    // LoggerFactory.getLogger(this.getClass).debug(s"👉  Initial partner block iterators:")
    // iterMap.filterNot(_._2 == PartnerBlockIterator.Empty).foreach { case (k, v) =>
    //   LoggerFactory.getLogger(this.getClass).debug(s"     $k: $v")
    // }

    val x = init(quantaMap, iterMap, seq, validpartners(allPartners, quantaMap)) match {
      case (s, _) if s.isEmpty => new Empty(allPartners)
      case (partnerSeq, remainingTime) => {
        new BlockIteratorImpl(allPartners, quantaMap, partnerSeq, remainingTime, iterMap)
      }
    }

    // x.toList(activeList).foreach { b =>
    //   LoggerFactory.getLogger(this.getClass).debug(s" Block:     $b")
    // }

    x

  }
}
