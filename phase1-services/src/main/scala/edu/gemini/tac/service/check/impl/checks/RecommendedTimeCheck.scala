package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.{check => api}
import edu.gemini.tac.persistence.phase1.{TimeAmount, TimeUnit}

import edu.gemini.tac.persistence.Implicits._
import edu.gemini.tac.service.check.impl.core.{ProposalIssue, StatelessProposalCheckFunction}

import ProposalIssue.noIssues
import edu.gemini.tac.persistence.{ObservingMode, ProposalIssueCategory, Proposal}
import edu.gemini.tac.persistence.ObservingMode.{CLASSICAL, QUEUE}
import edu.gemini.tac.persistence.phase1.proposal.{ClassicalProposal, QueueProposal}

object RecommendedTimeCheck extends StatelessProposalCheckFunction {
  val name        = "Recommended Time"
  val description =
    "Partner recommended time should be at least the PI requested time."

  // Margin of error.  Awarded time is allowed to be this much less than
  // requested time. (ITAC-266)
  val classicalMargin = new TimeAmount(new java.math.BigDecimal(0.5), TimeUnit.NIGHT)
  val queueMargin     = new TimeAmount(new java.math.BigDecimal(0.1), TimeUnit.HR)
  private val marginMap: Map[ObservingMode, TimeAmount] = Map(
    CLASSICAL -> classicalMargin,
    QUEUE     -> queueMargin
  )


  def noRequestedTimeMessage = "PI requested time  not specified."
  def noRequestedTime(p: Proposal) = ProposalIssue.error(this, p, noRequestedTimeMessage, ProposalIssueCategory.TimeAllocation)

  def noRecommendedTimeMessage = "Partner recommended time not specified."
  def noRecommendedTime(p: Proposal) = ProposalIssue.error(this, p, noRecommendedTimeMessage, ProposalIssueCategory.TimeAllocation)

  def negativeRecommendedTimeMessage = "Partner recommended time cannot be negative."
  def negativeRecommendedTime(p: Proposal) = ProposalIssue.error(this, p, negativeRecommendedTimeMessage, ProposalIssueCategory.TimeAllocation)

  def lowRecommendedTimeMessage(rec: TimeAmount, req: TimeAmount) =
    "Sum of partner recommended time (%.2f %s) less than total PI minimum request (%.2f %s).".format(rec.getValue, rec.getUnits.value(), req.getValue, req.getUnits.value())
  def lowRecommendedTime(p: Proposal, rec: TimeAmount, req: TimeAmount) =
    ProposalIssue.warning(this, p, lowRecommendedTimeMessage(rec, req), ProposalIssueCategory.TimeAllocation)

  def tooMuchTimeMessage(rec: TimeAmount) =
    "Proposal is allocated a large amount of time: '%s' hours".format(rec.convertTo(TimeUnit.HR))
  def tooMuchTime(p: Proposal, rec: TimeAmount) =
    ProposalIssue.warning(this, p, tooMuchTimeMessage(rec), ProposalIssueCategory.TimeAllocation)

  val zeroTime = new TimeAmount(new java.math.BigDecimal(0.0), TimeUnit.MIN)


  case class Times(recommended: TimeAmount, requested: TimeAmount) {

    // Adds a bit of margin of error.  Recommended times are allowed to be a
    // bit lower than requested.  ITAC-266
    private def augmentedRecommended(p: Proposal): TimeAmount = {
      recommended + marginMap({
        p.getPhaseIProposal match {
          case qp : QueueProposal => QUEUE
          case cp : ClassicalProposal => CLASSICAL
          case _ => QUEUE
        }
      })
    }

    def recommendedLessThanRequested(p: Proposal): Boolean =
      // save the lookup and addition if not necessary...
      (recommended ltTime requested) && (augmentedRecommended(p) ltTime requested)
  }

  def apply(p: Proposal): Set[api.ProposalIssue] = {
    val rec = p.getTotalRecommendedTime
    val minReq = p.getTotalMinRequestedTime
    if (rec eqTime zeroTime)
      noIssues // ITAC-264
    else if (rec ltTime zeroTime)
      Set(negativeRecommendedTime(p))
    else if (Times(rec, minReq).recommendedLessThanRequested(p) && ! p.isJointComponent)
      Set(lowRecommendedTime(p, rec, minReq))
    else if (rec.convertTo(TimeUnit.HR).getValue.longValue() >= 100)
      Set(tooMuchTime(p, rec))
    else
      noIssues
  }

}