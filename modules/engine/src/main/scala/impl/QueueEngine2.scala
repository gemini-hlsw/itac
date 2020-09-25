package edu.gemini.tac.qengine.impl

import edu.gemini.tac.qengine.api.{BucketsAllocation, QueueCalc}
import edu.gemini.tac.qengine.api.config.QueueEngineConfig
import edu.gemini.tac.qengine.api.queue.ProposalQueue
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.ctx.{ Context, Partner }
import edu.gemini.tac.qengine.impl.resource.RaResourceGroup
import edu.gemini.tac.qengine.log.{ ProposalLog, RejectMessage }
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.QueueBand._
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import edu.gemini.tac.qengine.impl.block.BlockIterator
import edu.gemini.tac.qengine.util.BoundedTime
import edu.gemini.tac.qengine.impl.resource.TimeResourceGroup
import edu.gemini.tac.qengine.impl.resource.TimeResource
import edu.gemini.tac.qengine.impl.resource.SemesterResource
import edu.gemini.tac.qengine.impl.QueueCalcStage.Params

object QueueEngine2 extends edu.gemini.tac.qengine.api.QueueEngine {

  def calc(
    rawProposals: Map[QueueBand, List[Proposal]],
    queueTime:    QueueTime,
    config:       QueueEngineConfig,
    extras:       List[Proposal],
    removed:      List[Proposal]
  ): QueueCalc = {

    // Silently filter out proposals that are not at our site.
    val siteProposals: Map[QueueBand, List[Proposal]] =
      rawProposals.map { case (b, ps) =>
        b -> ps.filter(_.site == config.site)
      }

    // Ensure that everything is in a compatible band. For now we'll just throw if there's an issue.
    siteProposals.foreach { case (b, ps) => ps.foreach { p => QueueEngineBandProblems.unsafeCheckAll(p, b) }}

    // Find all the observations that don't participate in the queue process, because their time
    // needs to be subtracted from the initail RaResourceGroup (which happens on construction).
    val rolloverObs     = config.rollover.obsList
    val classicalObs    = siteProposals(QBand1).filter(_.mode == Mode.Classical).flatMap(_.obsList)
    val raResourceGroup = RaResourceGroup(config.binConfig).reserveAvailable(rolloverObs ++ classicalObs)._1

    // We're done with classical proposals. Filter them out.
    val queueProposals: Map[QueueBand, List[Proposal]] =
      siteProposals.map { case (b, ps) =>
        b -> ps.filter(_.mode != Mode.Classical)
      }

    //Sets parameters for Band1
    def band1(grouped: Map[Partner, List[Proposal]], log: ProposalLog, qtime: QueueTime, config: QueueEngineConfig, bins: RaResourceGroup) = {
      // Calculate for the full time category (bands 1 and 2)
      val cat = Category.B1_2

      // Initialize an empty, starting queue state.
      val queue = ProposalQueueBuilder(qtime, QBand1)

      // Create a new block iterator that will step through the proposals
      // according to partner sequence and time quantum
      val iter = BlockIterator(config.partners, qtime.partnerQuanta, config.partnerSeq.sequence, grouped, _.obsList)

      // Create the initial restricted bins.  Percent bins are mapped to a
      // percentage of guaranteed queue time, and time bins set their own bound.
      val rbins = config.restrictedBinConfig.mapTimeRestrictions(
        percent => BoundedTime(qtime.full * percent),
        time => BoundedTime(time))
      val time = new TimeResourceGroup(rbins.map(new TimeResource(_)))
      val semRes = new SemesterResource(bins, time, QBand1)

      new Params(cat, queue, iter, _.obsList, semRes, log)
    }

    // Ok now let's do the Band 1 queue.
    val stage1: QueueCalcStage =
      QueueCalcStage(
        band1(
          grouped = queueProposals(QBand1).groupByPartnerAndSortedByRanking,
          log     = ProposalLog.Empty,
          qtime   = queueTime,
          config  = config,
          bins    = raResourceGroup
        )
      )

    // Done
    new QueueCalcImpl(
      context           = config.binConfig.context,
      queue             = stage1.queue,
      proposalLog       = stage1.log,
      bucketsAllocation = QueueEngine.BucketsAllocationImpl(stage1.resource.ra.grp.bins.toList)
    )

  }

  case class RemovedRejectMessage(prop: Proposal) extends RejectMessage {
    def reason: String = "Unknown."
    def detail: String = "Proposal was removed from consideration."
  }

  case class QueueCalcImpl(
    context:           Context,
    queue:             ProposalQueue,
    proposalLog:       ProposalLog,
    bucketsAllocation: BucketsAllocation
  ) extends QueueCalc

  implicit class ProposalListOps(self: List[Proposal]) {
    def groupByPartnerAndSortedByRanking: Map[Partner, List[Proposal]] =
      self.groupBy(_.ntac.partner).mapValues(_.sortBy(_.ntac.ranking))
  }

}

