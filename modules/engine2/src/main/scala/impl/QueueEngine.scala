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
import org.slf4j.LoggerFactory
import edu.gemini.tac.qengine.util.Time
import scalaz._, Scalaz._

object QueueEngine extends edu.gemini.tac.qengine.api.QueueEngine {
  private val Log = LoggerFactory.getLogger("edu.gemini.itac")

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

  /** Log the time availability and usage for a stage. */
  def show(config: QueueEngineConfig, s: QueueCalcStage): Unit = {
    Log.info(s"${Console.GREEN}-----------------------------------${Console.RESET}")
    Log.info(s"${Console.GREEN}Partner       Band 1/2       Band 3${Console.RESET}")
    config.partners.foreach { p =>
      val q   = s.queue
      val t0  = q.queueTime(p).toHours.value
      val b12Aval = q.queueTime(QueueBand.Category.B1_2, p).toHours.value
      val b12Used = q.usedTime(QueueBand.Category.B1_2, p).toHours.value
      val b3Aval = q.queueTime(QueueBand.Category.B3, p).toHours.value
      val b3Used = q.usedTime(QueueBand.Category.B3, p).toHours.value
      Log.info(f"${p.id}%-10s $b12Used%5.1f/$b12Aval%5.1f  $b3Used%5.1f/$b3Aval%5.1f")
    }
    Log.info(s"${Console.GREEN}-----------------------------------${Console.RESET}")
  }

  def calc(proposals: List[Proposal], queueTime: QueueTime, config: QueueEngineConfig, partners : List[Partner]): QueueCalc = {

    // (helper) run and log a queue calculation stage, using our local config.
    def stage(params: QueueCalcStage.Params): QueueCalcStage = {
      val stage = QueueCalcStage(params)
      show(config, stage)
      stage
    }

    // Identify the valid proposals and discard the others.
    val (validProposals, bins) = filterProposalsAndInitializeBins(proposals, config)
    val initialCandidates = ProposalPrep(validProposals)

    // Run a queue calc stage for bands 1 and 2. This is the same way it used to work, but we're
    // only doing it once because we're not re-using the iterator.
    val stageWithBands12 = {
      val candidates = initialCandidates
      val params = QueueCalcStage.Params.band12(
        grouped = candidates.group,
        log     = candidates.log,
        qtime   = queueTime,
        config  = config,
        bins    = bins
      )
      stage(params)
    }

    // Now add the band 3 programs. We do this by pretending the used time in bands 1/2 is all the
    // time we had in those bands, which is kind of an ugly hack but it works. Everything goes into
    // band 3, up to partner time limits.
    val stageWithBands123 = {
      val candidates = initialCandidates.remove(stageWithBands12.queue.toList).band3(stageWithBands12.log)
      val params = QueueCalcStage.Params.band3(
        config       = config,
        grouped      = candidates.group,
        phase12bins  = stageWithBands12.resource,
        phase12log   = stageWithBands12.log,
        phase12queue = stageWithBands12.queue,
      )
      val s = stage(params)
      s
    }

    // Construct our final queue, which requires a bit of rejiggering.
    val finalQueue = {

      // The band 1/2 boundary is wrong using the old strategy so we'll regroup them. First collect
      // the band 1/2 programs IN ORDER.
      val band12proposals: List[Proposal] =
        stageWithBands123.queue.bandedQueue.get(QueueBand.QBand1).orZero ++
        stageWithBands123.queue.bandedQueue.get(QueueBand.QBand2).orZero

      // Now re-group bands 1 and 2 using partner-specific time buckets
      val band12map: Map[QueueBand, List[Proposal]] =
        config.partners.foldMap { pa =>

          // Within a given partner we can map used time to band
          def band(t: Time): QueueBand = {
            val b1 = queueTime(QueueBand.QBand1, pa).percent(105)
            val b2 = queueTime(QueueBand.QBand2, pa).percent(105)
            if (t <= b1) QueueBand.QBand1 else if (t <= (b1 + b2)) QueueBand.QBand2 else QueueBand.QBand3
          }

          // Go through all the proposals for this partner adding each to its band, based on the
          // accumulated used time. This is why they need to be in order above.
          band12proposals
            .filter(_.ntac.partner == pa)
            .foldLeft((Time.Zero, Map.empty[QueueBand, List[Proposal]])) { case ((t, m), p) =>
              val tʹ = t + p.time
              (tʹ, m |+| Map(band(tʹ) -> List(p)))
          } ._2

        }

      // Band4 is a simple calculation
      val band4 = PoorWeatherCalc(initialCandidates.remove(stageWithBands123.queue.toList).propList)

      // Ok so *replace* bands 1/2 and then add band 4 then [deterministically] scramble the proposals
      // within each band since at this point they're all created equal.
      val bandedQueueʹ  = stageWithBands123.queue.bandedQueue ++ band12map
      val bandedQueueʹʹ = bandedQueueʹ + (QueueBand.QBand4 -> band4)
      val bandedQueueʹʹʹ = bandedQueueʹʹ.map { case (k, v) => (k, v.sortBy(_.piName.reverse)) }
      new FinalProposalQueue(queueTime, bandedQueueʹʹʹ)

    }

    // And we're done.
    new QueueCalcImpl(
      context           = config.binConfig.context,
      queue             = finalQueue,
      proposalLog       = stageWithBands123.log,
      bucketsAllocation = BucketsAllocationImpl(stageWithBands123.resource.ra.grp.bins.toList)
    )

  }

}
