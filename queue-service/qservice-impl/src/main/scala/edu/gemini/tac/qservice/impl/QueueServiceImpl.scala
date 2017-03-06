package edu.gemini.tac.qservice.impl

import edu.gemini.tac.persistence.{Proposal => PsProposal}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue, ScienceBand}
import edu.gemini.tac.psconversion._

import edu.gemini.tac.qengine.api.{BucketsAllocation, QueueCalc}
import edu.gemini.tac.qengine.api.queue.ProposalQueue
import edu.gemini.tac.qengine.api.queue.time.PartnerTimeCalc
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.impl.QueueEngine
import edu.gemini.tac.qengine.p1.{Proposal, QueueBand}
import edu.gemini.tac.qservice.api.QueueService
import edu.gemini.tac.qservice.impl.load.ProposalLoader
import edu.gemini.tac.qservice.impl.persist.log.QueueLogger
import edu.gemini.tac.qservice.impl.persist.{PartnerTimeCalcPersister, BandingPersister}
import edu.gemini.tac.qservice.impl.queue.ConfigExtractor
import edu.gemini.tac.service.{AbstractLogService => PsLogService, ICommitteeService => PsCommitteeService}

import org.apache.log4j.{Level, Logger}

import scalaz._
import Scalaz._

import scala.collection.JavaConverters._
import scala.xml.{Node, Elem}

object QueueServiceImpl extends QueueService {
  private val Log = Logger.getLogger(QueueServiceImpl.getClass)
  private val ShortFallFillFactor = 0.8

  def fill(queue: PsQueue, committeService: PsCommitteeService, logService: PsLogService) {
    val impl = new QueueServiceImpl(queue, committeService, logService)

    impl.logErrors()
    val startTime = System.currentTimeMillis
    val calc = impl.genQueue
    val elapsedTime = System.currentTimeMillis - startTime
    impl.persist(calc)
    impl.logger.logCalcTime(elapsedTime)
    calc.foreach { c =>
      impl.logger.logQueue(c)
      impl.logTimeConsumption(c.queue, queue.getSite.getDisplayName)
      impl.logRaBuckets(c.bucketsAllocation)
    }
    impl.logRaDecBin()
    impl.logPartnerPercentages()
    impl.logShutdownConfiguration()
  }
}

import QueueServiceImpl._

private class QueueServiceImpl(val queue: PsQueue, val comService: PsCommitteeService, val logService: PsLogService) {
  val context    = ContextConverter.read(queue)
  val partners   = Partners.fromQueue(queue)
  val partnerMap = for {
    parts <- partners
  } yield parts.values.map(p => p.id -> p).toMap

  val proposalResults = for {
    ctx <- context
    par <- partnerMap
    com <- nullSafePersistence("Queue has no committee") { queue.getCommittee }
    ps  <- safePersistence { comService.getAllCommitteeProposalsForQueueCreation(com.getId) }
    pl  <- new ProposalLoader(ctx, par).load(ps.asScala.toList)
  } yield pl

  val proposals = for {
    pr <- proposalResults
  } yield (List.empty[Proposal]/:pr) { (lst,r) =>
    r match {
      case (_, Success(nel)) => nel.toList ::: lst
      case _                 => lst
    }
  }

  val configExt = for {
    ps <- proposals
    cf = ConfigExtractor(queue, ps)
  } yield cf

  val logger    = new QueueLogger(queue, logService)

  def logRaBuckets(b: BucketsAllocation): Unit =
    configExt.foreach { _.config.foreach { c => logger.logRaBuckets(b) } }

  def logRaDecBin(): Unit =
    configExt.foreach { _.config.foreach { c => logger.logRaDecCalc(c.binConfig) } }

  def logShutdownConfiguration(): Unit =
    configExt.foreach { _.config.foreach { c => logger.logShutdowns(c.binConfig.shutdowns, c.binConfig.site) } }

  /* ITAC-390 */
  def logTimeConsumption(queue : ProposalQueue, siteName : String) : Unit =
    partners.foreach { parts =>
      val elem : List[Elem] = parts.values.sortBy(_.id).map{p : Partner =>
        logTimeConsumption(queue, p, siteName)
      }
      logger.logTimeConsumption(<table id="time-consumption">
        <tr id="time-consumption-header-row"><th>Partner</th><th>Band 1</th><th>B1 %</th><th>Band 2</th><th>B2 %</th><th>Band 3</th><th>B3%</th><th>Used</th><th>Hours to fill</th></tr>
        {elem}
      </table>)
    }

  def logTimeConsumption(queue : ProposalQueue,  partner : Partner, siteName : String) : Elem = {
    val bands = queue.bandedQueue.keySet.toList.sortBy(_.number)

    val band1Tuple = bandElement(bands.find(_.number == 1), partner, queue, bands)
    val band2Tuple = bandElement(bands.find(_.number == 2), partner, queue, bands)
    val band3Tuple = bandElement(bands.find(_.number == 3), partner, queue, bands)

    val total = band1Tuple._2 + band2Tuple._2 + band3Tuple._2
    val totalEl = <td class="consumed">{"%5.1f".format(total)}</td>

    val band1El = band1Tuple._1
    val band2El = band2Tuple._1
    val band3El = band3Tuple._1

    val netPartnerHours = (partner.id, siteName) match {
      case ("CL", "North") => 0
      case ("UH", "South") => 0
      case _ => queue.queueTime.fullPartnerTime(partner).toHours.value
    }
    val shortfall = ShortFallFillFactor * netPartnerHours - total
    val shortfallEl = <td text-align="right" class="shortfall">{"%5.1f".format(shortfall)}</td>.toList

    (partner.id, siteName) match {
      case ("SUBARU", _) => <tr class="time-consumption-row"><td class="partner-id">{partner.id}</td>{band1El}{band2El}{band3El}{totalEl}<td class="shortfall">{"N/A"}</td></tr>
      case ("KECK", _) => <tr class="time-consumption-row"><td class="partner-id">{partner.id}</td>{band1El}{band2El}{band3El}{totalEl}<td class="shortfall">{"N/A"}</td></tr>
      case ("CL", "North") => <a/>
      case ("UH", "South") => <a/>
      case _ => <tr class="time-consumption-row"><td class="partner-id">{partner.id}</td>{band1El}{band2El}{band3El}{totalEl}{shortfallEl}</tr>
    }
  }

  private def cumulativeBandConsumptionAsPercentOfTotal(queueBand : QueueBand)(implicit partner : Partner, bands : List[QueueBand], queue : ProposalQueue) : Double = {
    val actualPartnerTime = queue.queueTime.fullPartnerTime(partner).toHours.value
    val usedInBand = queue.bounds(queueBand, partner).used.toHours.value / actualPartnerTime * 100.0

    val used = queueBand.number match {
      case 1 => usedInBand
      case higher => {
        val lowerBand = bands.find(_.number == higher - 1).get
        val lower = cumulativeBandConsumptionAsPercentOfTotal(lowerBand)
        usedInBand + lower
      }
    }
    used
  }

  private def bandElement(
       maybeQueueBand : Option[QueueBand], partner:Partner, queue : ProposalQueue,
       bands : List[QueueBand]) : (List[Node], Double) = maybeQueueBand match {
      case Some(qband) => {
        val used = queue.bounds(qband, partner).used
        /*
        Percentage consumed in this band versus the total all-band time available
        (e.g., "20% 40% 60%" not the 100% 100% 100% which you'd get from bounds().fillPercent)
         */
        val fullPartnerTime = queue.queueTime.fullPartnerTime(partner).toHours.value
        val percent = fullPartnerTime match {
          case 0.0 => "N/A"
          case _   =>
            partner.id match {
              case "KECK" => "N/A"
              case "SUBARU" => "N/A"
              case _ => "%5.0f".format(cumulativeBandConsumptionAsPercentOfTotal(qband)(partner, bands, queue))
            }
        }
        val usedFmt = "%5.1f".format(used.toHours.value)
        (<td class="used">{usedFmt}</td><td class="percent">{percent}</td>.toList, used.toHours.value)
      }
      case None => (<td/>.toList, 0)
  }

  def logPartnerPercentages(): Unit = {
    configExt.foreach { _.config.foreach { c => logger.logPartnerConfiguration(c.partnerSeq.configuration) } }
  }

  def logErrors(): Unit = {
    import logger.logError

    configExt.swap.foreach(logError)
    configExt.foreach { ext =>
      ext.queueTime.swap.foreach(logError)
      ext.config.swap.foreach(logError)
    }

    proposalResults.swap.foreach(logError)
    proposalResults.foreach { results =>
      results.foreach {
        case (psProposal, Failure(messages)) => logger.logProposalLoadError(psProposal, messages)
        case _                               => // skip
      }
    }
  }

  def genQueue: Option[QueueCalc] =
    (for {
      parts     <- partners
      props     <- proposals
      ext       <- configExt
      queueTime <- ext.queueTime
      config    <- ext.config
    } yield QueueEngine.calc(props, queueTime, config, parts.values)).toOption

  def persist(calcOpt: Option[QueueCalc]): Unit =
    calcOpt.foreach(persist)

  private def persist(calc: QueueCalc): Unit = {
    logResults(calc)

    def proposalPool: List[(PsProposal, Proposal)] =
      proposalResults.toList.flatten.flatMap {
        case (ps, Success(props)) => Stream.continually(ps).zip(props.toList)
        case _                    => Nil // ignore failures here, we're trying to gather up all the valid proposals
      }

    val bp = new BandingPersister(calc.context, proposalPool)
    bp.persist(calc.queue, queue)

    configExt.foreach { _.partnerTimeCalc.foreach(persist) }
  }

  private def persist(ptc: PartnerTimeCalc): Unit =
    partners.foreach { parts =>
      PartnerTimeCalcPersister.persist(ptc, parts.psPartnerFor, queue)
    }

  private def logResults(calc: QueueCalc): Unit = {
    calc.proposalLog.toList.zipWithIndex.foreach(t => Log.log(Level.DEBUG, "LogEntry[%s]:%s".format(t._2, t._1)))
    Log.log(Level.DEBUG, calc.proposalLog.toXML)
    logger.logDecisions(calc)
    /*
    // ITAC-68: Store in reverse order for now so that results appear correctly
    // in the UI (which reverses the order).
    proposalLog.toList.reverse foreach {
      entry =>
        val key = entry.key
        entry.msg match {
          case msg: RejectMessage => logger.logSkipProposal(key, msg)
          case msg: AcceptMessage => logger.logAddProposal(key, msg)
          case _                  => // skip
        }
    }
    */
  }

}