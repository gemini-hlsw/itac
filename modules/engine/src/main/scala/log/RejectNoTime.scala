package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.Proposal
import xml.Elem

/**
 * A proposal rejection message for proposals that have no awarded time.
 */
object RejectNoTime {
  val name        = "No Time Award"
  def description(p: Proposal) = s"No awarded time, or time is not usable at ${p.site}."
}
final case class RejectNoTime(prop: Proposal) extends RejectMessage {
  def reason: String = RejectNoTime.name
  def detail: String = RejectNoTime.description(prop)

  override def subToXML : Elem = <NoTimeAward><Proposal id= { prop.id.toString }/></NoTimeAward>

}