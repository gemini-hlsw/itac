package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.{check => api}
import edu.gemini.tac.service.check.impl.core.StatelessProposalCheckFunction
import edu.gemini.tac.service.check.impl.core.ProposalIssue._

import edu.gemini.tac.persistence.Implicits._
import edu.gemini.tac.persistence.phase1.proposal.QueueProposal

//import edu.gemini.tac.persistence.RichProposal._

import scala.collection.JavaConversions._

//import IfJoint._

import edu.gemini.tac.persistence.phase1.{TimeAmount, TimeUnit}
import edu.gemini.model.p1.mutable.Band
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}

object Band3MinimumTimeCheck extends StatelessProposalCheckFunction {
  val name = "Band3 Minimum Time"

  val description =
    "Band 3 minimum time is equal to or less than total NTAC recommended time."

  val margin = new TimeAmount(new java.math.BigDecimal(0.1), TimeUnit.HR)

  private def hrs(t: TimeAmount): Double = t.convertTo(TimeUnit.HR).getValue.doubleValue()

  def nonPositiveMinimumMessage(t: TimeAmount): String =
    "Band 3 time (%.2f hrs) must be more than 0 hours.".format(hrs(t))

  def nonPositiveMinimumIssue(p: Proposal, t: TimeAmount): api.ProposalIssue =
    error(this, p, nonPositiveMinimumMessage(t), ProposalIssueCategory.TimeAllocation)

  def minimumMoreThanRecommendedMessage(b3: TimeAmount, rec: TimeAmount): String =
    "Band 3 time (%.2f hrs) must be less than or equal to total recommended time (%.2f hrs).".format(hrs(b3), hrs(rec))

  def minimumMoreThanRecommendedIssue(p: Proposal, b3: TimeAmount, rec: TimeAmount): api.ProposalIssue =
    error(this, p, minimumMoreThanRecommendedMessage(b3, rec), ProposalIssueCategory.TimeAllocation)

  private val zeroHours = new TimeAmount(new java.math.BigDecimal(0.0), TimeUnit.HR)

  private def checkNonPositiveMinimumIssue(p: Proposal, b3Min: TimeAmount): Option[api.ProposalIssue] =
    if (b3Min leTime zeroHours) Some(nonPositiveMinimumIssue(p, b3Min)) else None

  private def checkMoreThanRecommendedIssue(p: Proposal, b3Min: TimeAmount, rec: TimeAmount): Option[api.ProposalIssue] = {
    if ((rec gtTime zeroHours) && ((rec + margin) ltTime b3Min)) Some(minimumMoreThanRecommendedIssue(p, b3Min, rec)) else None
  }

  private def checkIssues(p: Proposal, b3Min: TimeAmount, recommended: TimeAmount): Option[api.ProposalIssue] = {
    checkNonPositiveMinimumIssue(p, b3Min) orElse
      checkMoreThanRecommendedIssue(p, b3Min, recommended) orElse
      None
  }

  def apply(p: Proposal): Set[api.ProposalIssue] =
    if (p.isBand3 && !p.isJointComponent) {
      // Try to safely determine the band 3 time
      val band3Time = p.getPhaseIProposal match {
        case q: QueueProposal => Option(q.getBand3Request).map(_.getMinTime)
        case _ => Some(p.getTotalTimeForBand(Band.BAND_3))
      }
      band3Time.map { b3t =>
        val recommended = p.getTotalRecommendedTime
        val issue = checkIssues(p, b3t, recommended)
        if (issue.isDefined) Set(issue.get) else noIssues
      }.getOrElse(noIssues) // strictly this is an error but there is another for that case
    } else {
      noIssues
    }

  //  def apply(p: Proposal): Set[api.ProposalIssue] =
  //      noIssues


  //  flatMap { b3 =>
  //        checkIssues(p, band3MinTime(b3))
  //      } map { issue =>
  //        Set(issue)
  //      } getOrElse noIssues
  //    }
}