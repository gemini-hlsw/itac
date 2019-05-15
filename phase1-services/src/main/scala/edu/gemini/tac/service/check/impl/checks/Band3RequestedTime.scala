package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.persistence.phase1.proposal.QueueProposal
import edu.gemini.tac.persistence.{Proposal, ProposalIssueCategory}
import edu.gemini.tac.service.check.impl.core.ProposalIssue._
import edu.gemini.tac.service.check.impl.core.StatelessProposalCheckFunction
import edu.gemini.tac.service.{check => api}

object Band3RequestedTime extends StatelessProposalCheckFunction {
  val name        = "Band3 requested time"
  val description =
    "Proposals with band 3 must have a requested time."

  def missingRequestedTime(p: Proposal): api.ProposalIssue =
    error(this, p, "Proposal with band3 observations did not request band 3 time", ProposalIssueCategory.TimeAllocation)

  def apply(p: Proposal): Set[api.ProposalIssue] =
    p.getPhaseIProposal match {
      case q: QueueProposal if p.isBand3 && !p.isJointComponent =>
        Option(q.getBand3Request) match {
          case Some(_) => noIssues
          case None => Set(missingRequestedTime(p))
      }
      case _ => noIssues
    }

}


