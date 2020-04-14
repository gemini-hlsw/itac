package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.Proposal
import xml.Elem

/**
 * A proposal rejection message for proposals that have no awarded time.
 */
object RejectNoTime {
  val name        = "No Time Award"
  val description = "NTAC did not award time to the proposal."
}
final case class RejectNoTime(prop: Proposal) extends RejectMessage {
  def reason: String = RejectNoTime.name
  def detail: String = RejectNoTime.description

  override def subToXML : Elem = <NoTimeAward><Proposal id= { prop.id.toString }/></NoTimeAward>

}