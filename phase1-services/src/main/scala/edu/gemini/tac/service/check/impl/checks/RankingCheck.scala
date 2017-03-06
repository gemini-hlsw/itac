package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.{check => api}
import edu.gemini.tac.service.check.impl.core.{StatefulProposalCheckFunction, ProposalIssue}

import ProposalIssue.noIssues

import java.math.BigDecimal
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}
import edu.gemini.tac.persistence.phase1.submission.{NgoSubmission, Submission}
import edu.gemini.tac.persistence.joints.JointProposal
import edu.gemini.tac.persistence.phase1.proposal.QueueProposal


object RankingCheck extends StatefulProposalCheckFunction[Set[PartnerRanking]] {
  val name         = "Partner Ranking"
  val description  = "Each proposal submitted by the same partner must have a unique numeric ranking."
  def initialState = Set.empty

  def duplicateMessage(r: PartnerRanking): String =
    "Proposal with duplicate ranking %s.".format(r)

  def duplicate(p: Proposal, r: PartnerRanking): Set[api.ProposalIssue] =
    singleError(p, duplicateMessage(r), ProposalIssueCategory.Structural)

  def badRankingMessage(rankingStr: String): String =
    Option(rankingStr) map { s=>
      "Illegal proposal ranking value '%s'".format(s)
    } getOrElse "Proposal missing partner ranking."

  def badRanking(p: Proposal, ranking: BigDecimal): Set[api.ProposalIssue] = {
    val rankingStr = if (ranking == null) "<null>" else ranking.toString
    singleError(p, badRankingMessage(rankingStr), ProposalIssueCategory.Structural)
  }

  type State = Set[PartnerRanking]

  private def checkRanking(r: PartnerRanking, p: Proposal, s: State): (Set[api.ProposalIssue], State) =
    if (s.contains(r))
      (duplicate(p, r), s)
    else if (r.rank == null)
      (badRanking(p, r.rank), s)
    else
      (noIssues, s + r)

  def apply(p: Proposal, s: State): (Set[api.ProposalIssue], State) =
    p match {
      case jp:JointProposal => (noIssues, s)
      case _ => {
        p.getPhaseIProposal match {
          case qp:QueueProposal => {
            checkRanking(PartnerRanking(qp.getPrimary.getPartner, qp.getPrimary.getAccept.getRanking, qp.getSite.getDisplayName), p, s)
          }
          case _ => (noIssues, s)
        }
      }
    }

}