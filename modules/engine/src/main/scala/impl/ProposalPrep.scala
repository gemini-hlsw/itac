package edu.gemini.tac.qengine.impl

import edu.gemini.tac.qengine.log._
import edu.gemini.tac.qengine.p1.{QueueBand, Proposal}
import edu.gemini.tac.qengine.ctx.Partner

/**
 * A ProposalPrep is used to filter and massage a list of proposals into a
 * usable form for the QueueEngine.  It contains a list of proposals and a
 * map of Proposal.Id to LogMessage for any proposals that are rejected.
 */
final case class ProposalPrep(propList: List[Proposal], band: QueueBand, log: ProposalLog) {

  /**
   * Groups the proposal list into a map of lists keyed by partner.  The lists
   * contain the partner's proposals sorted by ranking.
   */
  def group: Map[Partner, List[Proposal]] =
    propList.groupBy(_.ntac.partner).mapValues(_.sortBy(_.ntac.ranking))

  /**
   * Returns a new ProposalPrep that does not contain any of the indicated
   * proposals.  Calculates (this.propList -- rmProps) in a more efficient way.
   */
  def remove(rmProps: List[Proposal]): ProposalPrep = {
    val ids = rmProps.map(_.id).toSet
    new ProposalPrep(propList.filterNot(prop => ids.contains(prop.id)), band, log)
  }
}
