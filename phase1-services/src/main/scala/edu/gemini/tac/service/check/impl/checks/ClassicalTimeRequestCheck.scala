package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.{check => api}
import edu.gemini.tac.service.check.impl.core.{ProposalIssue, StatelessProposalCheckFunction}
import ProposalIssue.noIssues

import edu.gemini.tac.persistence.RichProposal._
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}
import edu.gemini.tac.persistence.phase1.TimeUnit
import java.lang.Boolean
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal

object ClassicalTimeRequestCheck extends StatelessProposalCheckFunction {
  def name        = "Classical Time Request"
  def description = "Classical time requests should be made in nights."

  val e = 0.000001

  def notMultipleOfTenMessage(hrs: Double ) =
    "Classical proposal requested %.2f hours.".format(hrs)
  def notMultipleOfTen(p: Proposal, hrs: Double): Set[api.ProposalIssue] =
    singleWarning(p, notMultipleOfTenMessage(hrs), ProposalIssueCategory.TimeAllocation)

  def isDefinitelyClassical(p: Proposal): Boolean = p.getPhaseIProposal.isClassical

  def requestedTimeUnits(p: Proposal): Option[TimeUnit] =
    for {
      sc : PhaseIProposal <- Option(p.getPhaseIProposal)
      units <- Option(sc.getTotalRecommendedTime.getUnits)
    } yield units

  def nonNightsIssues(p: Proposal, units: TimeUnit): Set[api.ProposalIssue] =
    p.totalRequestedTime map { t =>
      val hrs    = t.convertTo(TimeUnit.HR)
      val intHrs = hrs.getValue.toBigInteger().intValue()

      if ((math.abs(hrs.getValue.doubleValue() - intHrs ) < e) && (intHrs % 10 == 0))
        noIssues
      else
        notMultipleOfTen(p, hrs.getValue.doubleValue())
    } getOrElse noIssues

  def classicalIssues(p: Proposal): Set[api.ProposalIssue] =
    (for {
      units <- requestedTimeUnits(p)
      if (units != TimeUnit.NIGHT)
    } yield nonNightsIssues(p, units)) getOrElse noIssues

  def apply(p: Proposal): Set[api.ProposalIssue] =
    if (isDefinitelyClassical(p).booleanValue()) classicalIssues(p) else noIssues
}