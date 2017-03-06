package edu.gemini.tac.persistence

import phase1._

import proposal.PhaseIProposal
import scala.collection.JavaConverters._
import submission.Submission

/**
 * Adds methods to make a proposal easier to use and avoid NullPointerException.
 */
final class RichProposal(p: Proposal) {

  def phaseIProposal : Option[PhaseIProposal] =
    Option(p.getPhaseIProposal)

  def pi: Option[PrincipalInvestigator] =
    for {
      pip <- phaseIProposal
      inv <- Option(pip.getInvestigators)
      pri <- Option(inv.getPi)
    } yield pri

  def observations: List[Observation] =
    (for {
      pip <- phaseIProposal
      lst  <- Option(pip.getObservations)
    } yield lst.asScala.toList) getOrElse Nil

  def totalRequestedTime: Option[TimeAmount] =
    for {
      pip <- phaseIProposal
      timeAmount <- Option(pip.getTotalRequestedTime)
    } yield timeAmount

  def submissionsList: List[Submission] =
    (for {
      pip <- phaseIProposal
      ngoSubs <- Option(pip.getSubmissions)
    } yield ngoSubs.asScala.toList) getOrElse Nil

  def totalRecommendedTime: Option[TimeAmount] =
    for {
      pip <- phaseIProposal
      timeAmount <- Option(pip.getTotalRecommendedTime)
    } yield timeAmount

  def flaggedSubmission: Option[Submission] =
    for {
      pip <- phaseIProposal
      flaggedSubmission <- Option(pip.getPrimary)
    } yield flaggedSubmission

  def submissionPartner: Option[Partner] =
    for {
      fs      <- flaggedSubmission
      partner <- Option(fs.getPartner)
    } yield partner

  def targets: List[Target] =
    (for {
      pip <- phaseIProposal
      targets <- Option(pip.getTargets)
    } yield targets.asScala.toList) getOrElse Nil
}

object RichProposal {
  implicit val toRichProposal = (x: Proposal) => new RichProposal(x)
}
