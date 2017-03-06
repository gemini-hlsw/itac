package edu.gemini.tac.service.check.impl.core

import edu.gemini.tac.service.{check => api}
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}

/**
 * Extends proposal check with some helpers for making singleton Set results.
 */
trait BaseProposalCheckFunction extends api.ProposalCheck {

  /**
   * Creates a singleton set containing an error proposal issue.
   */
  def singleError(p: Proposal, msg: String,  cat : ProposalIssueCategory): Set[api.ProposalIssue] =
    Set(ProposalIssue.error(this, p, msg, cat))

  /**
   * Creates a singleton set containing a warning proposal issue.
   */
  def singleWarning(p: Proposal, msg: String, cat : ProposalIssueCategory): Set[api.ProposalIssue] =
    Set(ProposalIssue.warning(this, p, msg, cat))
}