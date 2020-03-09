// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.impl

import block.{Block, BlockIterator}
import edu.gemini.tac.qengine.impl.resource.SemesterResource
import edu.gemini.tac.qengine.log.{AcceptMessage, RejectMessage}
import queue.ProposalQueueBuilder
import edu.gemini.tac.qengine.p1.{Observation, Proposal}
import edu.gemini.tac.qengine.p1.QueueBand.Category
import org.slf4j.LoggerFactory

/**
 * QueueFrame represents the state of the queue generation process at a
 * particular step of the block iterator.  QueueFrames are pushed onto a "stack"
 * until a problem with a proposal is encountered, at which point all frames
 * from the point at which the rejected proposal was introduced are removed and
 * the proposal is skipped.
 */
final case class QueueFrame(
  val queue: ProposalQueueBuilder,
  val iter: BlockIterator,
  val res: SemesterResource
) {
  private val LOGGER            = LoggerFactory.getLogger(this.getClass)
  private val applicationLogger = LoggerFactory.getLogger(getClass())

  val lName = LOGGER.getName

  case class Next(frame: QueueFrame, accept: Option[AcceptMessage])

  def isStartOf(prop: Proposal): Boolean = iter.isStartOf(prop)

  def skip(activeList: Proposal => List[Observation]): QueueFrame =
    new QueueFrame(queue, iter.skip(activeList), res)

  def hasNext: Boolean = iter.hasNext

  private def updated(block: Block): (ProposalQueueBuilder, Option[AcceptMessage]) =
    if (block.isFinal) {
      // There will be no more blocks for this proposal, so accept it.
      val prop     = block.prop
      val partner  = prop.id.partner
      val newQueue = queue :+ prop
      applicationLogger.debug("  💚  Block was accepted. 👍")
      (newQueue, Some(AcceptMessage(prop, newQueue.bounds(partner), newQueue.bounds)))
    } else {
      // More blocks for this proposal so we can't accept it yet.
      applicationLogger.debug(
        "  ⚠️  So far so good, but there are more blocks so we can't accept yet."
      )
      (queue, None)
    }

  private def logBlock(block: Block) = {
    val msg =
      s"  👉  Proposing a block of ${block.time.toHours} for a ${block.obs.time.toHours} obs in ${Console.BOLD}${block.prop.id.reference}${Console.RESET}, which was awarded ${block.prop.ntac.awardedTime.toHours} by ${block.prop.ntac.partner.id}."
    LOGGER.debug(msg)
    //applicationLogger.info("next():" + block.toString);
  }

  def next(activeList: Proposal => List[Observation]): RejectMessage Either Next = {
    LOGGER.debug("  👉  Next frame.")
    val (block, newIter) = iter.next(activeList)
    logBlock(block)
    res.reserve(block, queue) match {
      case Right(r) =>
        val (updatedQueue, accept) = updated(block)
        LOGGER.debug(s"  💚  Success: ${accept.map(_.detail).getOrElse("«no message»")}")
        Right(Next(new QueueFrame(updatedQueue, newIter, r), accept))
      case Left(e) =>
        LOGGER.debug(s"  ❌  Failed: ${e.detail}")
        Left(e)
    }
  }

  // Should stop when there are no more time blocks to iterate over or when
  // we have successfully scheduled enough proposals to move to a new
  // category.
  def emptyOrOtherCategory(cat: Category): Boolean = {
    val noMoreQueueFrames = !this.hasNext
    val finishedBand      = !this.queue.band.isIn(cat)

    if (noMoreQueueFrames) LOGGER.debug("👉  Last block in the iterator.")
    if (finishedBand) LOGGER.debug(s"👉  Finished with ${queue.band}.")

    noMoreQueueFrames || finishedBand
  }

  def toXML = <QueueFrame/>
}
