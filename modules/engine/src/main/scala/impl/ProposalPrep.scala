package edu.gemini.tac.qengine.impl

import edu.gemini.tac.qengine.log._
import edu.gemini.tac.qengine.p1.{QueueBand, Proposal}
import edu.gemini.tac.qengine.ctx.Partner

/**
 * A ProposalPrep is used to filter and massage a list of proposals into a
 * usable form for the QueueEngine.  It contains a list of proposals and a
 * map of Proposal.Id to LogMessage for any proposals that are rejected.
 */
final class ProposalPrep private (val propList: List[Proposal], val cat: QueueBand.Category, val log: ProposalLog) {

  /**
   * Groups the proposal list into a map of lists keyed by partner.  The lists
   * contain the partner's proposals sorted by ranking.
   */
  def group: Map[Partner, List[Proposal]] =
    propList.groupBy(_.ntac.partner).mapValues(_.sortBy(_.ntac.ranking))

  private def filter(p: (Proposal) => Boolean, l: (Proposal) => LogMessage): ProposalPrep =
    propList.partition(p) match {
      case (goodProps, badProps) => new ProposalPrep(goodProps, cat, log.updated(badProps, cat, l))
    }

  /**
   * Reduces the proposal list to those proposals that contain observations,
   * logging rejection messages for those that do not contain observations.
   */
  private def withObservations = filter(_.obsList.nonEmpty, RejectNoObs.apply)
  // TODO: also rejects proposals with only band3observations -- okay?
  // I don't think we should allow it to try to schedule a proposal with
  // no band 1/2 observations in bands 1 and 2 -- it seems like it would
  // probably accept it right away.  Ideally it would only try to schedule a
  // proposal with only band 3 observations during the band 3 phase.  As
  // implemented above though, for a proposal to even be considered in band 3
  // it had better have band 1/2 observations ...

  /**
   * Reduces the proposal list to those proposals that have awarded time.
   */
  private def withTime = filter(!_.time.isZero, RejectNoTime.apply)

  /**
   * Reduces the proposal list to those that are available in Band 3, logging
   * rejection messages for those that are not available.
   */
  private def availableInBand3 = filter(_.band3Observations.nonEmpty, RejectNotBand3.apply)

  /**
   * Creates a new ProposalPrep suitable for use in the Band 3 QueueCalcStage.
   * It removes all proposals that are not available for band 3 scheduling.
   */
  def band3(newLog: ProposalLog): ProposalPrep =
    new ProposalPrep(propList, QueueBand.Category.B3, newLog).availableInBand3

  def band3(newLog: Option[ProposalLog] = None): ProposalPrep =
    band3(newLog.getOrElse(log))

  /**
   * Returns a new ProposalPrep that does not contain any of the indicated
   * proposals.  Calculates (this.propList -- rmProps) in a more efficient way.
   */
  def remove(rmProps: List[Proposal]): ProposalPrep = {
    val ids = Proposal.expandJoints(rmProps).map(_.id).toSet
    new ProposalPrep(propList.filterNot(prop => ids.contains(prop.id)), cat, log)
  }
}

object ProposalPrep {
  def apply(propList: List[Proposal], log: ProposalLog = ProposalLog.Empty): ProposalPrep = {
    // Expand any joint proposals into their parts.
    val expanded = Proposal.expandJoints(propList)

    // Filter out duplicate ids.  There are several parts of the code that
    // assume each proposal has a unique id.
    val unique   = expanded.groupBy(_.id).values.map(_.head).toList

    val prep = new ProposalPrep(unique, QueueBand.Category.B1_2, log)
    val withTime = prep.withTime
    val withObservations = withTime.withObservations
    withObservations
  }
}
