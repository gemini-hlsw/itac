package edu.gemini.tac.qengine.impl

import block.BlockIterator
import edu.gemini.tac.qengine.p1.{Proposal, QueueBand, Observation}
import QueueBand.Category
import annotation.tailrec
import resource._
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import edu.gemini.tac.qengine.log.{RejectCategoryOverAllocation, ProposalLog}
import edu.gemini.tac.qengine.api.config.QueueEngineConfig
import edu.gemini.tac.qengine.util.BoundedTime._
import edu.gemini.tac.qengine.util.BoundedTime
import edu.gemini.tac.qengine.api.queue.time.{PartnerTime, QueueTime}
import edu.gemini.tac.qengine.ctx.Partner
import java.util.logging.{Level, Logger}

object QueueCalcStage {
  type Result = (QueueFrame, ProposalLog)

  private val Log = Logger.getLogger(this.getClass.getName)

  object Params {
    //Sets parameters for Band1 and 2
    def apply(props: ProposalPrep, qtime: QueueTime, config: QueueEngineConfig, bins: RaResourceGroup) = {
      // Calculate for the full time category (bands 1 and 2)
      val cat = Category.B1_2

      // Initialize an empty, starting queue state.
      val queue = ProposalQueueBuilder(qtime)

      // Create a new block iterator that will step through the proposals
      // according to partner sequence and time quantum
      val iter = BlockIterator(config.partners, qtime.partnerQuanta, config.partnerSeq.sequence, props.group, _.obsList)

      // Create the initial restricted bins.  Percent bins are mapped to a
      // percentage of guaranteed queue time, and time bins set their own bound.
      val rbins = config.restrictedBinConfig.mapTimeRestrictions(
        percent => BoundedTime(qtime.full * percent),
        time => BoundedTime(time))
      val time = new TimeResourceGroup(rbins.map(new TimeResource(_)))
      val band = new BandResource(config.restrictedBinConfig.bandRestrictions)
      val semRes = new SemesterResource(bins, time, band)
      Log.log(Level.FINE, semRes.toXML.toString())

      new Params(cat, queue, iter, _.obsList, semRes, props.log)
    }

    //This is the Band3 Constructor. Uses info from the passed-in queue calc stage.
    def apply(partners: List[Partner],  props: ProposalPrep, config: QueueEngineConfig, pass1: QueueCalcStage, clean: QueueCalcStage, partnerQuanta : PartnerTime) = {
      // Use the clean queue state that was previously calculated as the starting
      // point.
      val queue = clean.queue

      // Put all in Band 3
      val cat = Category.B3

      // Use the bins from the clean calculation.  We will be adding on to
      // them as we go, but the previous proposals are still in the queue and
      // still need to be tracked.
      val semRes = clean.resource

      // Create a new block iterator that starts where we left off with the
      // partner sequence, using the new band 3 proposal pool.
      val prevIter = pass1.iter
      /*
      However! There is the possibility that the previous iterator is now empty (i.e., B1/B2 calculation
      accepted or rejected every proposal). If that's true, we want to reset the block iterator, since
      we *may* have some proposals that are schedulable in B3 (i.e., rejected in B1/B2 due to conditions)
       */
      val iter =
        prevIter.quantaMap == PartnerTime.empty(partners) match {
          case false => BlockIterator(partners, prevIter.quantaMap, prevIter.seq, props.group, _.band3Observations)
          case true => BlockIterator(partners, partnerQuanta, config.partnerSeq.sequence, props.group, _.band3Observations)
        }
      new Params(cat, queue, iter, _.band3Observations, semRes, props.log)
    }
  }

  class Params(val cat: QueueBand.Category,
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
  @tailrec private def compute(cat: Category, stack: List[QueueFrame], log: ProposalLog, activeList : Proposal=>List[Observation]): Result = {
    val stackHead = stack.head
    Log.log(Level.FINE, stackHead.toXML.toString())
    if (stackHead.emptyOrOtherCategory(cat)) {
      Log.log(Level.FINE, "Stack is empty [" + ! stackHead.hasNext + "] or in other category [Expected : " + cat + " Actual: " + stackHead.queue.band + "]")
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
    val result = compute(p.cat, queueFrameHead, p.log, p.activeList)
    new QueueCalcStage(p.cat, result)
  }
}

/**
 * Contains the result of calculating (a portion of) the queue.
 */
final class QueueCalcStage private(cat: Category, result: QueueCalcStage.Result) {
  private val frame: QueueFrame = result._1

  val resource = frame.res
  val iter = frame.iter

  // Take the queue and log from the result and run the band restriction filter
  // again -- merging proposals can cause movement in the queue and band
  // restriction violations.
  val (queue, log) =
    if (cat == Category.B3) frame.res.band.filter(frame.queue, result._2)
    else (frame.queue, result._2)
}
