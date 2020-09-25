package edu.gemini.tac.qengine.impl

import block.BlockIterator
import edu.gemini.tac.qengine.p1.{Proposal, QueueBand, Observation}
import annotation.tailrec
import resource._
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import edu.gemini.tac.qengine.log.{RejectCategoryOverAllocation, ProposalLog}
import edu.gemini.tac.qengine.p1.QueueBand
import org.slf4j.LoggerFactory

object QueueCalcStage {
  type Result = (QueueFrame, ProposalLog)

  private val Log = LoggerFactory.getLogger("edu.gemini.itac")

  class Params(val cat: QueueBand,
               val queue: ProposalQueueBuilder,
               val iter: BlockIterator,
               val activeList : Proposal=>List[Observation],
               val res: SemesterResource,
               val log: ProposalLog)


  private def rollback(stack: List[QueueFrame], prop: Proposal, activeList: Proposal => List[Observation]): List[QueueFrame] =
    stack.dropWhile(!_.isStartOf(prop)) match {
      case head :: tail => head.skip(activeList) :: tail
      case Nil => sys.error("Tried to skip a proposal that wasn't seen.")
    }


  //
  // Recurse through calling stack.head.next until hasNext returns false or we move to a
  // different queue band time category.  For each result, if a QueueFrame,
  // push it on the stack.  That means that we successfully recorded the time
  // block without hitting any bin limits.
  //
  // If a RejectMessage, log it and pop the stack until and including the
  // frame for which "isBeginningOf()" the proposal associated with the
  // RejectMessage. Call skip on that frame and push it so that it considers
  // the next proposal in the sequence.
  //
  @tailrec private def compute(cat: QueueBand, stack: List[QueueFrame], log: ProposalLog, activeList : Proposal=>List[Observation]): Result = {
    val stackHead = stack.head
    if (!stackHead.hasNext) {
      Log.trace( "Stack is empty [" + ! stackHead.hasNext + "]")
      (stackHead, log.updated(stackHead.iter.remPropList, cat, RejectCategoryOverAllocation(_, cat)))
    } else stackHead.next(activeList) match {
      case Left(msg) => //Error, so roll back (and recurse)
        compute(cat, rollback(stack, msg.prop, activeList), log.updated(msg.prop.id, cat, msg), activeList)
      case Right(frameNext) => //OK, so accept (and recurse)
        val updatedLog = frameNext.accept.map(msg => log.updated(msg.prop.id, cat, msg)).getOrElse(log)
        compute(cat, frameNext.frame :: stack, updatedLog, activeList)
    }
  }

  /**
   * Calculates the corresponding portion of the queue according to the
   * provided parameters:
   * p.cat : Category -> The current band (essentially)
   * p.queue : ProposalQueueBuilder -> Encapsulates the state of the being-built queue
   * p.iter : BlockIterator -> Produces a block of time for the current partner's preferred Proposal
   * p.res :  SemesterResource -> Encapsulates the state of the remaining resources
   * p.log :  ProposalLog -> Holds a record of decisions
   * p.activeList : Proposal=>List[Observation] -> Either _.obsList or _.band3Observations
   *
   * Begins recursive call to compute(ProposalQueueBuilder, BlockIterator, SemesterResource)
   */
  def apply(p: Params): QueueCalcStage = {
    val queueFrameHead = List(new QueueFrame(p.queue, p.iter, p.res))
    Log.info(s"Constructing a QueueCalcStage with cat=${p.cat}")
    val result = compute(p.cat, queueFrameHead, p.log, p.activeList)
    new QueueCalcStage(result)
  }
}

/**
 * Contains the result of calculating (a portion of) the queue.
 */
final class QueueCalcStage private(result: QueueCalcStage.Result) {
  private val frame: QueueFrame = result._1

  val resource = frame.res
  val iter = frame.iter

  // Take the queue and log from the result and run the band restriction filter
  // again -- merging proposals can cause movement in the queue and band
  // restriction violations.
  val (queue, log) = (frame.queue, result._2)
}
