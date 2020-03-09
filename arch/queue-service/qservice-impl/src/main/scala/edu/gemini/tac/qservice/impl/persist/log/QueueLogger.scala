package edu.gemini.tac.qservice.impl.persist.log

import edu.gemini.qengine.skycalc.RaBinSize
import edu.gemini.tac.persistence.{LogEntry => PsLogEntry, Proposal => PsProposal}
import edu.gemini.tac.persistence.LogEntry.Type._
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.qengine.api.config.{Shutdown, SiteSemesterConfig}
import edu.gemini.tac.qengine.api.{BucketsAllocation, QueueCalc}
import edu.gemini.tac.qengine.ctx.Site
import edu.gemini.tac.qengine.log.{ProposalDetailMessage, ProposalLog}
import edu.gemini.tac.qservice.impl.shutdown.ShutdownCalc
import edu.gemini.tac.service.{AbstractLogService => PsLogService}

import org.apache.log4j.{Level, Logger}
import collection.JavaConverters._
import scala.xml.Elem
import edu.gemini.tac.psconversion.{BadData, DatabaseError, PsError}
import scalaz.NonEmptyList

/**
 * Logs messages associated with a queue.
 */
class QueueLogger(val queue: PsQueue, val logService: PsLogService) {
  private val LOGGER = Logger.getLogger(this.getClass)
  private def format(key: ProposalLog.Key, msg: ProposalDetailMessage): String =
    "<b>%s</b> %-8s - %-26s - %s".format(key.id, key.cat, msg.reason, msg.detail)

  /**
   * Logs a proposal added message.
   */
  def logAddProposal(key: ProposalLog.Key, msg: ProposalDetailMessage) {
    log(format(key, msg), PROPOSAL_ADDED)
  }

  /**
   * Logs a proposal skipped message.
   */
  def logSkipProposal(key: ProposalLog.Key, msg: ProposalDetailMessage) {
    log(format(key, msg), PROPOSAL_SKIPPED)
  }

  /**
   *  Logs an error message.
   */
  def logError(msg: String) { log(msg, QUEUE_ERROR)}

  def logError(error: PsError): Unit = {
    error match {
      case DatabaseError(t) => logError("Persistence Model Error: " + t.getMessage)
        LOGGER.error("PersistenceModel Error", t)
      case BadData(msg)     => logError(msg)
    }
  }


  def logProposalLoadError(psProposal: PsProposal, messages: NonEmptyList[String]): String = {
    def getId: String = {
      def getNonJointId(p: PsProposal): String =
        s"${p.getPartnerAbbreviation} - ${p.getPartnerReferenceNumber}"

      try {
        psProposal.getProposals.asScala.map(getNonJointId).mkString(", ")
      } catch {
        case ex: Exception =>
          LOGGER.error("Database model problem determining proposal id", ex)
          "UNKNOWN ID"
      }
    }
    s"$getId: ${messages.list.toList.mkString("\n")}"
  }

  /**
   * Log queue generation time.
   */
  def logCalcTime(ms: Long) {
    val msg = "Calculated queue in %.2f seconds.".format(ms / 1000.0)
    log(msg, PROPOSAL_QUEUE_GENERATED)
  }

  def logQueue(calc: QueueCalc) {
    val key   = QueueTimeTable.keyTable(calc.queue).toString()
    val data  = QueueTimeTable.queueTable(calc).toString()
    val msg = "%s\n%s".format(key, data)
    log(msg, PROPOSAL_QUEUE_GENERATED)
  }

  def logDecisions(calc: QueueCalc) {
    log(DecisionTable(calc).table.toString(), PROPOSAL_QUEUE_GENERATED)
  }

  def logRaDecCalc(siteSemester: SiteSemesterConfig) {
    val tables = RaDecCalcTables(siteSemester)
    log(tables.raTable.toString() + tables.decTable.toString(), PROPOSAL_QUEUE_GENERATED)
  }

  def logRaBuckets(buckets: BucketsAllocation) {
    log(buckets.raTables, PROPOSAL_QUEUE_GENERATED)
  }

  def logPartnerConfiguration(partnerConfigurationRepresentation : Elem) {
    val msg = partnerConfigurationRepresentation.label match {
      case "CustomPartnerSequence"       => "Custom Partner Sequence: (" + partnerConfigurationRepresentation.attributes("name").text + ")"
      case "ProportionalPartnerSequence" => "Proportional Partner Sequence"
      case _                             => partnerConfigurationRepresentation.label
    }
    log(msg, PROPOSAL_QUEUE_GENERATED)
  }

  def logShutdowns(shutdowns : List[Shutdown], queueSite : Site) {
    val qShutdowns = shutdowns.filter(_.site == queueSite)
    val times = qShutdowns.map(s => ShutdownCalc.asTime(s, RaBinSize.DEFAULT)) // TODO: really, default all the time?
    val shutdownTotal = times.foldLeft(0.0)((accum,v) => accum + v.toHours.value)
    log("Shutdowns removed %1.2f hours from %s queue".format(shutdownTotal, queueSite.toString), PROPOSAL_QUEUE_GENERATED)
  }

  def logTimeConsumption(elem : Elem) {
    log(elem.toString(), PROPOSAL_QUEUE_GENERATED)
  }

  private def log(msg: String, logType: PsLogEntry.Type) {
    LOGGER.log(Level.DEBUG, msg)
    val entry = logService.createNewForQueue(msg, Set(logType).asJava, queue)
    logService.addLogEntry(entry)
  }
}