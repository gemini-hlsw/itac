package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.util.BoundedTime

/**
 * Proposal acceptance message.
 */
object AcceptMessage {
  private def detailTemplate = "%s rank %5s. %-42s %-42s"
  def detail(prop: Proposal, pBounds: BoundedTime, tBounds: BoundedTime): String = {
    val p = "Partner: " + LogMessage.formatBoundedTime(pBounds) + "."
    val o = "Overall: " + LogMessage.formatBoundedTime(tBounds) + "."
    detailTemplate.format(prop.id.partner, prop.ntac.ranking.format, p, o)
  }
}

case class AcceptMessage(prop: Proposal, partnerBounds: BoundedTime, totalBounds: BoundedTime) extends LogMessage with ProposalDetailMessage {
  val reason: String = "Accepted"
  val detail: String = AcceptMessage.detail(prop, partnerBounds, totalBounds)

  override def subToXML =
    <AcceptMessage>
      <Reason>Accepted</Reason>
      <Detail>{AcceptMessage.detail(prop, partnerBounds, totalBounds)}</Detail>
      <AcceptedProposal>
        <Ranking partner={prop.ntac.partner.toString}>{ prop.ntac.ranking.format }</Ranking>
        { prop.toXML }</AcceptedProposal>
      <PartnerBounds>{ partnerBounds.toXML }</PartnerBounds>
      <TotalBounds>{ totalBounds.toXML }</TotalBounds>
  </AcceptMessage>
}