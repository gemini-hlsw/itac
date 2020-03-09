package edu.gemini.tac.qservice.impl.persist

import edu.gemini.tac.qengine.p1.QueueBand._
import edu.gemini.tac.persistence.{Proposal => PsProposal}
import edu.gemini.tac.persistence.joints.{JointProposal => PsJointProposal}
import edu.gemini.tac.persistence.queues.{Banding => PsBanding, JointBanding => PsJointBanding}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.queues.{ScienceBand => PsScienceBand}
import PsScienceBand._
import edu.gemini.tac.qengine.p1.{JointProposalPart, JointProposal, Proposal, QueueBand}
import edu.gemini.tac.qengine.p2.ProgramId

import java.util.logging.{Level, Logger}

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._


/**
 * Converts a Queue Engine QueueBand to a persistence ScienceBand.
 */
object PsBandingFactory {
  val Log = Logger.getLogger(getClass.getName)

  val bandMap: Map[QueueBand, PsScienceBand] =
    Map(QBand1 -> BAND_ONE, QBand2 -> BAND_TWO, QBand3 -> BAND_THREE, QBand4 -> POOR_WEATHER)

  private def mkNonJointBanding(band: PsScienceBand, prop: PsProposal, queue: PsQueue): PsBanding =
    new PsBanding(queue, prop, band)

  private def mkJointBanding(band: PsScienceBand, prop: PsProposal, queue: PsQueue): PsJointBanding =
    prop match {
      case jp: PsJointProposal => new PsJointBanding(queue, jp, band)
      case _                   => sys.error("expecting a joint proposal here")
    }
}

import PsBandingFactory._

class PsBandingFactory(psBand: PsScienceBand, pool: List[(PsProposal, Proposal)], queue: PsQueue) extends ((Proposal, Int, ProgramId) => PsBanding) {
  import PsBandingFactory._

  def this(band: QueueBand, pool: List[(PsProposal, Proposal)], queue: PsQueue) =
    this(PsBandingFactory.bandMap(band), pool, queue)

  private def findJointPart(ps: PsProposal, jp: JointProposalPart): Option[PsProposal] =
    ps.getProposals.asScala.find { psPart =>
      (psPart.getPartner.getPartnerCountryKey === jp.ntac.partner.id) &&
        (psPart.getPartnerReferenceNumber === jp.ntac.reference)
    }

  private def notFound(ps: PsProposal, jp: JointProposalPart): Nothing = {
    val msg =  s"Could not find matching persistence proposal for: ${jp.id} in ${ps.getId}"
    Log.log(Level.SEVERE, msg)
    sys.error(msg)
  }


  val idMap = pool.flatMap { case (ps, p) =>
    p match {
      case jp: JointProposal =>
        val partEntries = jp.toParts.map { part => part.id -> findJointPart(ps, part).fold(notFound(ps, part))(identity) }
        (partEntries/:jp.toParts) { (lst, part) => (Proposal.Id(part.ntac.partner, jp.jointIdValue) -> ps) :: lst }
      case _ => List(p.id -> ps)
    }
  }.toMap

  def apply(prop: Proposal, mergeIndex: Int, progId: ProgramId): PsBanding = {
    val banding: PsBanding = prop match {
      case jp: JointProposal =>
        val jb      = mkJointBanding(psBand, idMap(prop.id), queue)
        val psParts = jp.toParts.map(part => idMap(part.id))
        psParts.foreach { ps => jb.add(ps) }
        jb
      case _ =>
        mkNonJointBanding(psBand, idMap(prop.id), queue)
    }
    banding.setMergeIndex(mergeIndex)
    banding.setProgramId(progId.toString)
    banding
  }
}
