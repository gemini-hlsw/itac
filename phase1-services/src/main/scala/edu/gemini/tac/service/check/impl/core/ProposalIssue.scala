package edu.gemini.tac.service.check.impl.core

import edu.gemini.tac.service.{check => api}
import api.ProposalIssue.Severity
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}
import javax.management.remote.rmi._RMIConnection_Stub

/**
 * Simple implementation of the ProposalIssue interface.
 */
case class ProposalIssue(severity: Severity, check: api.ProposalCheck, proposal: Proposal, message: String, category : ProposalIssueCategory) extends api.ProposalIssue

object ProposalIssue {
  val noIssues = Set.empty[api.ProposalIssue]

  /**
   * Provides a shortcut for making an error issue.
   */
  val error = ProposalIssue(Severity.error, _: api.ProposalCheck, _: Proposal, _: String, _: ProposalIssueCategory)

  /**
   * Provides a shortcut for making a warning issue.
   */
  val warning = ProposalIssue(Severity.warning, _: api.ProposalCheck, _: Proposal, _: String, _: ProposalIssueCategory)

}