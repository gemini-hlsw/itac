package edu.gemini.tac.qengine.impl

import block.BlockIterator
import edu.gemini.tac.qengine.p1.{Proposal, QueueBand, Observation}
import QueueBand.Category
import annotation.tailrec
import resource._
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import edu.gemini.tac.qengine.log.{RejectCategoryOverAllocation, ProposalLog}
import edu.gemini.tac.qengine.api.config.QueueEngineConfig
// import edu.gemini.tac.qengine.util.BoundedTime._
import edu.gemini.tac.qengine.util.BoundedTime
import edu.gemini.tac.qengine.api.queue.time.{PartnerTime, QueueTime}
import edu.gemini.tac.qengine.ctx.Partner
// import java.util.logging.{Level, Logger}
import edu.gemini.tac.qengine.util.Time
// import edu.gemini.tac.qengine.api.queue.time.ExplicitQueueTime
// import edu.gemini.tac.qengine.api.config.QueueBandPercentages
import edu.gemini.tac.qengine.util.Percent
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.p1.QueueBand._
import org.slf4j.LoggerFactory

object QueueCalcStage {
  type Result = (QueueFrame, ProposalLog)

  private val Log = LoggerFactory.getLogger("edu.gemini.itac")

  object Params {

    //Sets parameters for Band1 and 2
    def band12(grouped: Map[Partner, List[Proposal]], log: ProposalLog, qtime: QueueTime, config: QueueEngineConfig, bins: RaResourceGroup) = {
      // Calculate for the full time category (bands 1 and 2)
      val cat = Category.B1_2

      // Initialize an empty, starting queue state.
      val queue = ProposalQueueBuilder(qtime, ??? : QueueBand)

      // Create a new block iterator that will step through the proposals
      // according to partner sequence and time quantum
      val iter = BlockIterator(config.partners, qtime.partnerQuanta, config.partnerSeq.sequence, grouped, _.obsList)

      // Create the initial restricted bins.  Percent bins are mapped to a
      // percentage of guaranteed queue time, and time bins set their own bound.
      val rbins = config.restrictedBinConfig.mapTimeRestrictions(
        percent => BoundedTime(qtime.full * percent),
        time => BoundedTime(time))
      val time = new TimeResourceGroup(rbins.map(new TimeResource(_)))
      val semRes = new SemesterResource(bins, time, ??? : QueueBand)

      new Params(cat, queue, iter, _.obsList, semRes, log)
    }


    def band3(phase12queue: ProposalQueueBuilder, grouped: Map[Partner, List[Proposal]], phase12log: ProposalLog, config: QueueEngineConfig, phase12bins: SemesterResource) = {
      // ok so now we want to set the band 1/2 cutoffs to be whatever the used time is.
      val hackedQueueTime: QueueTime =
        new QueueTime {
          val delegate = phase12queue.queueTime
          def fullPartnerTime: PartnerTime = delegate.fullPartnerTime
          // def bandPercentages: QueueBandPercentages = delegate.bandPercentages
          def overfillAllowance(band: QueueBand) = delegate.overfillAllowance(band)
          def full: Time = delegate.full
          def partnerTime(band: QueueBand): PartnerTime = ???
          def partnerPercent(p: Partner): Percent = ???
          // def apply(band: QueueBand): Time =
          //   band match {
          //     case QBand1 | QBand2 => phase12queue.usedTime(band)
          //     case QBand3 | QBand4 => delegate(band)
          //   }
          def apply(band: QueueBand, p: Partner): Time = ???
        }
      val hackedQueue = phase12queue.copy(queueTime = hackedQueueTime)
      val iter = BlockIterator(config.partners, phase12queue.queueTime.partnerQuanta, config.partnerSeq.sequence, grouped, _.band3Observations)
      new Params(Category.B3, hackedQueue, iter, _.band3Observations, phase12bins.copy(band = QBand3), phase12log)
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
