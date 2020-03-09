package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.{check => api}

import edu.gemini.tac.service.check.impl.core.{ProposalIssue, StatelessProposalCheckFunction}
import ProposalIssue.noIssues
import scala.collection.JavaConversions._
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}

object SubmissionPartnerCheck extends StatelessProposalCheckFunction {
  val name        = "Submission Partners"
  val description = "Each proposal should have a single submission partner."

  def noSubmissionFlagsMessage: String =
    "Proposal has no partner submissions with an accept part."
  def noSubmissionFlags(p: Proposal): Set[api.ProposalIssue] =
    singleError(p, noSubmissionFlagsMessage, ProposalIssueCategory.Structural)

  def multipleSubmissionFlagsMessage(n: Int): String =
    "Proposal has multiple (%d) partner submissions with an accept part.".format(n)
  def multipleSubmissionFlags(p: Proposal, n: Int): Set[api.ProposalIssue] =
    singleError(p, multipleSubmissionFlagsMessage(n), ProposalIssueCategory.Structural)

  def apply(p: Proposal): Set[api.ProposalIssue] =
    if (!p.isJoint()) {
      p.getSubmissionsPartnerEntries count { ps =>
        val (partner, submission) = ps
        submission.getAccept != null } match {
        case 0 => noSubmissionFlags(p)
        case 1 => noIssues
        case n => multipleSubmissionFlags(p, n)
      }
    } else {
      noIssues
    }
}