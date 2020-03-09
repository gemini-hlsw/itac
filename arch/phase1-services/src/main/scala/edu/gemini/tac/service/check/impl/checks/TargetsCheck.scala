package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.{check => api}
import edu.gemini.tac.service.check.impl.core.{ProposalIssue, StatelessProposalCheckFunction}

import edu.gemini.tac.persistence.RichProposal._

import ProposalIssue.noIssues
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}

object TargetsCheck extends StatelessProposalCheckFunction {
  val name        = "Targets"
  val description = "Proposal should contain targets."

  def noTargetsMessage = "Proposal contains no targets."
  def noTargets(p: Proposal) = singleWarning(p, noTargetsMessage, ProposalIssueCategory.Structural)

  def apply(p: Proposal): Set[api.ProposalIssue] =
    if (p.targets.size == 0) noTargets(p) else noIssues
}