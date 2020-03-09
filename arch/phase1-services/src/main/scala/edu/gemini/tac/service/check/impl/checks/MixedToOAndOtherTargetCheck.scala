package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.check.impl.core.StatelessProposalCheckFunction
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}
import edu.gemini.tac.service.check.ProposalIssue
import edu.gemini.tac.service.check.impl.core.ProposalIssue.noIssues
import edu.gemini.tac.persistence.RichProposal._
import edu.gemini.tac.persistence.phase1.TooTarget

/**
 * UX-1374 "Add proposal warning for any proposal that contains both too and non/sidereal targets."
 *
 */

object MixedToOAndOtherTargetCheck extends StatelessProposalCheckFunction {
  val name        = "Mixture of ToO and Sidereal/Nonsidereal Targets"
  val description = "Proposal should not contain both ToO targets and non-ToO targets."

  def mixedMessage = "Proposal contains both ToO targets and sidereal/nonsidereal targets."
  def mixedTargets(p: Proposal) = singleWarning(p, mixedMessage, ProposalIssueCategory.Structural)


  def apply(p: Proposal): Set[ProposalIssue] = {
   p.targets.exists(t => t.isInstanceOf[TooTarget]) && p.targets.exists(t => ! t.isInstanceOf[TooTarget]) match {
      case true => mixedTargets(p)
      case false => noIssues
   }
  }
}