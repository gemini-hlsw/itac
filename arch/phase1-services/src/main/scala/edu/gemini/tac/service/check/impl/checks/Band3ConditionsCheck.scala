package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.{check => api}
import edu.gemini.tac.service.check.impl.core.StatelessProposalCheckFunction
import edu.gemini.tac.persistence.phase1.{Condition, Observation}
import edu.gemini.tac.persistence.phase1.Target

//import edu.gemini.tac.persistence.Implicits._       // NOTE: this implicits define the order of conditions!

import scala.collection.JavaConversions._
import edu.gemini.model.p1.mutable.Band
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}

object Band3ConditionsCheck extends StatelessProposalCheckFunction {
  val name        = "Band3 Conditions"

  val description =
    "Band 3 conditions must be equal to or worse than any Band 1 or 2 condition used in an observation in the proposal."

  def message(b12obs: (Target, Condition),  b3obs: (Target, Condition)): String =
    "Band 3 conditions (%s) for target %s not equal to or worse than Band 1 or 2 conditions (%s) for target %s.".
      format(b3obs._2.getName, b3obs._1.getName,
             b12obs._2.getName, b12obs._1.getName)
  def band3ConditionsIssue(t:(Proposal, (Target,  Condition), (Target, Condition))): api.ProposalIssue =
    singleError(t._1, message(t._2, t._3), ProposalIssueCategory.ObservingConstraint).head // This is ugly!

  private def observationsForBand(p: Proposal, band: Band): Set[Observation] =
    p.getObservations.filter { _.getBand == band }.toSet

  private def band12WorseThanBand3(p: Proposal): Set[(Proposal, (Target,  Condition), (Target, Condition))] = {
    val b12obs = observationsForBand(p, Band.BAND_1_2)
    val b3obs = observationsForBand(p, Band.BAND_3)
    for (b12 <- b12obs; b3 <- b3obs
         if b12.getTarget.equals(b3.getTarget) && b3.getCondition.isBetterThan(b12.getCondition))
      yield (p, (b12.getTarget, b12.getCondition), (b3.getTarget, b3.getCondition()));
  }

  def apply(p: Proposal): Set[api.ProposalIssue] = {
    for (t <- band12WorseThanBand3(p)) yield band3ConditionsIssue(t)
  }
}