// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.impl

import queue.FinalProposalQueue
import resource._

import collection.immutable.List
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.api.{BucketsAllocation, QueueCalc}
import edu.gemini.tac.qengine.log.ProposalLog
import edu.gemini.tac.qengine.api.queue.ProposalQueue
import edu.gemini.tac.qengine.ctx.{Context, Partner}
import edu.gemini.tac.qengine.api.config.{ConditionsCategory, QueueEngineConfig, SiteSemesterConfig}
import edu.gemini.tac.qengine.p2.rollover.RolloverObservation

import collection.immutable.List._
import edu.gemini.tac.qengine.api.queue.time.{PartnerTimes, QueueTime}

import edu.gemini.tac.qengine.util.BoundedTime
import org.slf4j.LoggerFactory
import edu.gemini.spModel.core.Site

object QueueEngine extends edu.gemini.tac.qengine.api.QueueEngine {

  // N.B. needs to be lazy because we're mucking around with system properties to configure logging
  // and it happens *after* this class is initialized. :-\
  private lazy val Log = LoggerFactory.getLogger(getClass())

  case class RaAllocation(name: String, boundedTime: BoundedTime)

  case class BucketsAllocationImpl(raBins: List[RaResource]) extends BucketsAllocation {

    sealed trait Row extends Product with Serializable
    case class RaRow(h: String, r: Double, l: Double) extends Row
    case class ConditionsRow(t: ConditionsCategory, r: Double, l: Double) extends Row

    val hPerBin  = 24 / raBins.length
    val binHours = 0 until 24 by 24 / raBins.length
    val raRanges = binHours.map(h => s"$h-${h + hPerBin} h")
    val report = raRanges.zip(raBins).toList.map {
      case (h, b) =>
        val ra = RaRow(
          h,
          math.round(b.remaining.toMinutes.value) / 60.0,
          math.round(b.limit.toMinutes.value) / 60.0
        )
        val conds = b.condsRes.bins.bins.toList.sortBy(_._1.name).map {
          case (c, t) =>
            ConditionsRow(
              c,
              math.round(t.remaining.toMinutes.value) / 60.0,
              math.round(t.limit.toMinutes.value) / 60.0
            )
        }
        //s"$ra\n${conds.mkString("\n")}"
        ra :: conds
    }

    override def toString = {
      report.mkString("\n")
      //BucketsAllocationImpl(Nil)
      //System.exit(0)
    }

    override val raTables = {
      <table>
        <tr><th colspan="4">RA/Conditions Bins (remaining/limit)</th></tr>
        <tr><td><b>Bin type</b></td><td><b>Remaining</b></td><td><b>Limit</b></td></tr>
        {
        report.flatten.collect {
          case RaRow(h: String, r: Double, l: Double) =>
            <tr border="2px">
                <td style="text-align:right">RA: {h}</td>
                <td style="text-align:right">{f"$r%1.2f"} hrs</td>
                <td style="text-align:right">{f"$l%1.2f"} hrs</td>
              </tr>
          case ConditionsRow(t: ConditionsCategory, r: Double, l: Double) =>
            <tr>
                <td style="text-align:right">Conditions: {t}</td>
                <td style="text-align:right">{f"$r%1.2f"} hrs</td>
                <td style="text-align:right">{f"$l%1.2f"} hrs</td>
              </tr>
        }
      }
      </table>
    }.toString

    val raTablesANSI: String =
      report.flatten.map {
        case RaRow(h, r, l)         => f"${Console.BOLD}RA: $h%-78s   $r%6.2f  $l%6.2f${Console.RESET}"
        case ConditionsRow(t, r, l) => f"Conditions: $t%-70s   $r%6.2f  $l%6.2f "
      } .mkString("\n")

  }

  private final class QueueCalcImpl(
    val context: Context,
    val queue: ProposalQueue,
    val proposalLog: ProposalLog,
    val bucketsAllocation: BucketsAllocation
  ) extends QueueCalc

  private[impl] def classicalProps(allProps: List[Proposal], site: Site): List[Proposal] =
    allProps filter { p =>
      (p.mode == Mode.Classical) && (p.site == site)
    }

  private[impl] def classicalObs(allProps: List[Proposal], site: Site): List[Observation] =
    classicalProps(allProps, site) flatMap { p =>
      p.relativeObsList(QueueBand.QBand1)
    }

  private[impl] def initBins(
    config: SiteSemesterConfig,
    rollover: List[RolloverObservation],
    props: List[Proposal]
  ): RaResourceGroup = {
    val cattimes = rollover ++ classicalObs(props, config.site)
    RaResourceGroup(config).reserveAvailable(cattimes)._1
  }

  private[impl] def initBins(config: QueueEngineConfig, props: List[Proposal]): RaResourceGroup =
    initBins(config.binConfig, config.rollover.obsList, props)

  // TODO:
  // If it becomes necessary, an alternative to running the band1/2 phase
  // a second time would be to calculate the difference in all the bins used
  // for partially scheduled proposals.  Would be a bit of work to figure out
  // but probably would run more efficiently.

  def apply(
    propList: List[Proposal],
    queueTime: QueueTime,
    config: QueueEngineConfig,
    partners: List[Partner]
  ): QueueCalc =
    calc(propList, queueTime, config, partners)

  /**
   * Filters out proposals from the other site. Initializes bins using that list. *Then* removes non-queue proposals.
   * Note that this means that the RaResourceGroup returned is initialized from non-queue and queue proposals
   */
  def filterProposalsAndInitializeBins(
    proposals: List[Proposal],
    config: QueueEngineConfig
  ): (List[Proposal], RaResourceGroup) = {
    // Remove any proposals for the opposite site w/o polluting the log.
    val siteProps = proposals.filter(_.site == config.binConfig.site)

    Log.debug(
      s"👉  Removing those not at ${config.binConfig.site}, we are down to ${siteProps.length} proposals."
    )

    // Compute the initial resource bins, pre-reserving rollover and classical
    // time in the corresponding categories.
    val bins = initBins(config, siteProps)

    // Remove any non-queue proposals.
    val queueOnly = siteProps.filter(_.mode.schedule)
    Log.debug(s"👉  Removing non-queue programs, we are down to ${queueOnly.length} proposals.")

    (queueOnly, bins)
  }

  def fillBands1And2(
    orderedProposals: List[Proposal],
    queueTime: QueueTime,
    config: QueueEngineConfig,
    bins: RaResourceGroup
  ): QueueCalcStage = {
    val proposalPrep = ProposalPrep(orderedProposals)
    QueueCalcStage(QueueCalcStage.Params(proposalPrep, queueTime, config, bins))
  }

  def fillBand3(
    partners: List[Partner],
    initialPrep: ProposalPrep,
    clean: QueueCalcStage,
    partiallyFilled: QueueCalcStage,
    config: QueueEngineConfig,
    partnerQuanta: PartnerTimes
  ): QueueCalcStage = {
    val band3ProposalCandidates = initialPrep.remove(clean.queue.toList).band3(partiallyFilled.log)
    val params = QueueCalcStage.Params(
      partners,
      band3ProposalCandidates,
      config,
      partiallyFilled,
      clean,
      partnerQuanta
    )
    QueueCalcStage(params)
  }

  //Remove all proposals previously scheduled in Bands 1-3.
  def unscheduledPoorWeatherProposals(
    initialPrep: ProposalPrep,
    stageWithBands123: QueueCalcStage
  ): List[Proposal] =
    PoorWeatherCalc(initialPrep.remove(stageWithBands123.queue.toList).propList)

  def buildFinalQueue(
    stageWithBands123: QueueCalcStage,
    band4: List[Proposal],
    queueTime: QueueTime
  ): ProposalQueue = {
    val bandMap = stageWithBands123.queue.bandedQueue.updated(QueueBand.QBand4, band4)
    new FinalProposalQueue(queueTime, bandMap)
  }

  def calc(
    proposals: List[Proposal],
    queueTime: QueueTime,
    config: QueueEngineConfig,
    partners: List[Partner]
  ): QueueCalc = {
    Log.debug(s"👉  I was given ${proposals.length} unfiltered proposals.")

    val (validQueueProps, bins) = filterProposalsAndInitializeBins(proposals, config)

    //We need to reuse this initial calculation for Bands 3 & 4
    val initialPrep = ProposalPrep(validQueueProps)

    Log.debug(s"1️⃣ 2️⃣  Filling bands 1 and 2...")

    // Run a queue calc stage for bands 1 and 2.  This will result in the
    // preliminary definition of bands 1 and 2, but will contain resource
    // reservations that were partially filled with band1/2 conditions.
    val partiallyFilled: QueueCalcStage =
      fillBands1And2(validQueueProps, queueTime, config, bins)

    Log.debug(
      s"👉  Done. There are now ${partiallyFilled.queue.toList.length} programs in the queue."
    )

    Log.debug(s"1️⃣ 2️⃣  Re-filling bands 1 and 2 to prevent partial reservation of resources...")

    // Re-execute the band 1 and 2 stage, using only the proposals that we know
    // will be included.  This will prevent the partial reservation of resources
    val stageWithBands12: QueueCalcStage =
      fillBands1And2(partiallyFilled.queue.toList, queueTime, config, bins)

    Log.debug(
      s"👉  Done. There are now ${stageWithBands12.queue.toList.length} programs in the queue."
    )
    Log.debug(s"1️⃣ 2️⃣ 3️⃣  Filling band 3...")

    // Prepare proposals for the band 3 phase.  Remove all that were previously
    // queued in bands 1 and 2, get rid of those that cannot be scheduled in
    // band 3.  Keep the log from the first pass and add to it.
    val stageWithBands123 = fillBand3(
      partners,
      initialPrep,
      stageWithBands12,
      partiallyFilled,
      config,
      queueTime.partnerQuanta
    )

    Log.debug(
      s"👉  Done. There are now ${stageWithBands123.queue.toList.length} programs in the queue."
    )
    Log.debug(s"4️⃣  Finding band 4 programs ..")

    // Figure out the poor weather proposals and return the result.
    val band4 = unscheduledPoorWeatherProposals(initialPrep, stageWithBands123)

    Log.debug(s"👉  Done. There are ${band4.length} band 4 programs.")
    Log.debug(s"1️⃣ 2️⃣ 3️⃣ 4️⃣  Building final queue...")

    // Complement the band 1,2,3 map with poor weather proposals and create
    // a final queue with it.
    val finalQueue = buildFinalQueue(stageWithBands123, band4, queueTime)

    Log.trace("Filtered %d to %d".format(proposals.size, validQueueProps.size))
    new QueueCalcImpl(
      config.binConfig.context,
      finalQueue,
      stageWithBands123.log,
      BucketsAllocationImpl(stageWithBands123.resource.ra.grp.bins.toList)
    )
  }
}
