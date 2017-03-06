package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.persistence.phase1.PartnerReference

import edu.gemini.tac.service.{check => api}
import edu.gemini.tac.service.check.impl.core.{StatefulProposalCheckFunction, ProposalIssue}

import ProposalIssue.noIssues
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}
import edu.gemini.tac.persistence.joints.JointProposal
import edu.gemini.tac.persistence.phase1.proposal.QueueProposal

object ReferenceCheck extends StatefulProposalCheckFunction[Set[PartnerReference]] {
  val name         = "Partner Reference"
  val description  = "Each proposal must have a unique partner reference."
  def initialState = Set.empty

  def duplicateMessage(r: PartnerReference): String =
    "Duplicate proposal with partner reference %s.".format(r)

  def duplicate(p: Proposal, r: PartnerReference): Set[api.ProposalIssue] =
    singleError(p, duplicateMessage(r), ProposalIssueCategory.Structural)

  def missingMessage = "Proposal missing partner reference information."
  def missing(p: Proposal): Set[api.ProposalIssue] = singleError(p, missingMessage, ProposalIssueCategory.Structural)

  type State = Set[PartnerReference]

  private def checkRef(r: PartnerReference, p: Proposal, s: State): (Set[api.ProposalIssue], State) =
    if (s.contains(r))
      (duplicate(p, r), s)
    else if (r.ref == null)
      (missing(p), s)
    else
      (noIssues, s + r)

  def apply(p: Proposal, s: State): (Set[api.ProposalIssue], State) =
    p match {
      case jp:JointProposal => (noIssues, s)
      case _ => {
        p.getPhaseIProposal match {
          case qp:QueueProposal => {
            checkRef(PartnerReference(qp.getPrimary.getPartner, qp.getSubmissionsKey, p.getSite.getDisplayName), p, s)
          }
          case _ => (noIssues, s)
        }
      }
    }

}