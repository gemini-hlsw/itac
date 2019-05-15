package edu.gemini.tac.psconversion

import edu.gemini.model.p1.immutable.{Proposal => P1Proposal}
import edu.gemini.model.p1.{mutable => p1mutable}

import edu.gemini.tac.persistence.{Proposal => PsProposal}

import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.p1.io.{JointIdGen, ProposalIo}

import edu.gemini.tac.util.HibernateToMutableConverter

import scalaz._
import Scalaz._

/**
 *
 */
class P1ModelConverter(partners: List[Partner]) {
  val io = new ProposalIo(partners.map(p => p.id -> p).toMap)

  /**
   * Load a persistence proposal into the qengine representation, if possible.
   *
   * @param ps persistence model proposal
   * @param when valid time for target coordinate calculation
   * @param idGen joint id generation
   * @return
   */
  def load(ps: PsProposal, when: Long, idGen: JointIdGen): PsError \/ (NonEmptyList[Proposal], JointIdGen) = {
    // Convert to the mutable proposal model if the database is willing.
    val mutable = safePersistence {
      HibernateToMutableConverter.toMutable(ps.getPhaseIProposal)
    }

    def cleanProposal(m: p1mutable.Proposal): P1Proposal = {
      val p1 = P1Proposal(m)
      p1.copy(observations = p1.nonEmptyObservations)
    }

    // Go back to the safe immutable model and extract the qengine proposal
    // information from that.
    mutable.flatMap { mut =>
      io.read(cleanProposal(mut), when, idGen).disjunction.leftMap(l => BadData(l.list.toList.mkString("\n")))
    }
  }

}
