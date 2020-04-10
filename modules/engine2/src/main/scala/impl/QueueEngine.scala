package edu.gemini.tac.qengine.impl

import queue.FinalProposalQueue
import resource._

import collection.immutable.List
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.api.{BucketsAllocation, QueueCalc}
import edu.gemini.tac.qengine.log.ProposalLog
import edu.gemini.tac.qengine.api.queue.ProposalQueue
import edu.gemini.tac.qengine.ctx.{Context, Partner}
import edu.gemini.spModel.core.Site
import edu.gemini.tac.qengine.api.config.{ConditionsCategory, QueueEngineConfig, SiteSemesterConfig}
import edu.gemini.tac.qengine.p2.rollover.RolloverObservation

import collection.immutable.List._
import edu.gemini.tac.qengine.api.queue.time.{PartnerTime, QueueTime}
import java.util.logging.{Level, Logger}

import edu.gemini.tac.qengine.util.BoundedTime

object QueueEngine extends edu.gemini.tac.qengine.api.QueueEngine {
  private val Log = Logger.getLogger(this.getClass.getName)

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


  private final class QueueCalcImpl(val context: Context, val queue: ProposalQueue, val proposalLog: ProposalLog, val bucketsAllocation: BucketsAllocation) extends QueueCalc

  private[impl] def classicalProps(allProps: List[Proposal], site: Site): List[Proposal] =
    allProps filter { p => (p.mode == Mode.Classical) && (p.site == site) }

  private[impl] def classicalObs(allProps: List[Proposal], site: Site): List[Observation] =
    classicalProps(allProps, site) flatMap { p => p.relativeObsList(QueueBand.QBand1) }

  private[impl] def initBins(config: SiteSemesterConfig, rollover: List[RolloverObservation], props: List[Proposal]): RaResourceGroup = {
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

  def apply(propList: List[Proposal], queueTime: QueueTime, config: QueueEngineConfig, partners: List[Partner]): QueueCalc =
    calc(propList, queueTime, config, partners)

  /**
   * Filters out proposals from the other site. Initializes bins using that list. *Then* removes non-queue proposals.
   * Note that this means that the RaResourceGroup returned is initialized from non-queue and queue proposals
   */
  private def filterProposalsAndInitializeBins(proposals: List[Proposal], config: QueueEngineConfig): (List[Proposal], RaResourceGroup) = {
    // Remove any proposals for the opposite site w/o polluting the log.
    val siteProps = proposals.filter(_.site == config.binConfig.site)

    // Compute the initial resource bins, pre-reserving rollover and classical
    // time in the corresponding categories.
    val bins = initBins(config, siteProps)

    // Remove any non-queue proposals.
    (siteProps.filter(_.mode.schedule), bins)
  }

  def show(config: QueueEngineConfig, s: QueueCalcStage): Unit = {
    import scalaz._, Scalaz._
    println(s"${Console.GREEN}-----------------------------------${Console.RESET}")
    println(s"${Console.GREEN}Partner       Band 1/2       Band 3${Console.RESET}")
    config.partners.foreach { p =>
      val q   = s.queue
      val t0  = q.queueTime(p).toHours.value
      val b12Aval = q.queueTime(QueueBand.Category.B1_2, p).toHours.value
      val b12Used = q.usedTime(QueueBand.Category.B1_2, p).toHours.value
      val b3Aval = q.queueTime(QueueBand.Category.B3, p).toHours.value
      val b3Used = q.usedTime(QueueBand.Category.B3, p).toHours.value
      println(f"${p.id}%-10s $b12Used%5.1f/$b12Aval%5.1f  $b3Used%5.1f/$b3Aval%5.1f")
    }
    println(s"${Console.GREEN}-----------------------------------${Console.RESET}")
  }

  def calc(proposals: List[Proposal], queueTime: QueueTime, config: QueueEngineConfig, partners : List[Partner]): QueueCalc = {
    val (validProposals, bins) = filterProposalsAndInitializeBins(proposals, config)

    //We need to reuse this initial calculation for Bands 3 & 4
    val initialPrep = ProposalPrep(validProposals)

    // Run a queue calc stage for bands 1 and 2.  This will result in the
    // preliminary definition of bands 1 and 2, but will contain resource
    // reservations that were partially filled with band1/2 conditions.
    val partiallyFilled = {
      val proposalPrep = ProposalPrep(validProposals)
      QueueCalcStage(QueueCalcStage.Params(proposalPrep.group, proposalPrep.log, queueTime, config, bins))
    }

    // show(config, partiallyFilled)

    // Re-execute the band 1 and 2 stage, using only the proposals that we know
    // will be included.  This will prevent the partial reservation of resources
    val stageWithBands12 = partiallyFilled // {
    //   val proposalPrep = ProposalPrep(partiallyFilled.queue.toList)
    //   QueueCalcStage(QueueCalcStage.Params(proposalPrep.group, proposalPrep.log, queueTime, config, bins))
    // }

    // looks like there's really no reason to do this again, the used times are the same
    show(config, stageWithBands12)

    val stageWithBands123 = {
      val band3ProposalCandidates = initialPrep.remove(stageWithBands12.queue.toList).band3(partiallyFilled.log)
      println(s"*** there are ${band3ProposalCandidates.propList.length} proposals that might go into Band3.")
      val params = QueueCalcStage.Params.band3(
        config       = config,
        grouped      = band3ProposalCandidates.group,
        phase12bins  = stageWithBands12.resource,
        phase12log   = stageWithBands12.log,
        phase12queue = stageWithBands12.queue,
      )
      QueueCalcStage(params)
    }

    show(config, stageWithBands123)
    // sys.exit(0)

    // Prepare proposals for the band 3 phase.  Remove all that were previously
    // queued in bands 1 and 2, get rid of those that cannot be scheduled in
    // band 3.  Keep the log from the first pass and add to it.
    // val stageWithBands123 = { // fillBand3(partners, initialPrep, stageWithBands12, partiallyFilled, config, queueTime.partnerQuanta)
    //   val band3ProposalCandidates = initialPrep.remove(stageWithBands12.queue.toList).band3(partiallyFilled.log)
    //   val params = QueueCalcStage.Params(partners, band3ProposalCandidates, config, partiallyFilled, stageWithBands12, queueTime.partnerQuanta)
    //   QueueCalcStage(params)
    // }

    // Complement the band 1,2,3 map with poor weather proposals and create
    // a final queue with it.
    val finalQueue = { //buildFinalQueue(stageWithBands123, band4, queueTime)
      val band4   = PoorWeatherCalc(initialPrep.remove(stageWithBands123.queue.toList).propList)
      val bandMap = stageWithBands123.queue.bandedQueue.updated(QueueBand.QBand4, band4)
      new FinalProposalQueue(queueTime, bandMap)
    }

    Log.log(Level.FINE, "Filtered %d to %d".format(proposals.size, validProposals.size))
    new QueueCalcImpl(config.binConfig.context, finalQueue, stageWithBands123.log, BucketsAllocationImpl(stageWithBands123.resource.ra.grp.bins.toList))
  }
}
