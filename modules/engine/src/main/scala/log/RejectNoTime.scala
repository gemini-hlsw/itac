package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.Proposal

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
}