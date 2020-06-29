package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.Proposal

/**
 * A proposal rejection message for proposals that contain no observations.
 */
object RejectNoObs {
  val name        = "No Observations"
  val description = "Proposal contains no observations."
}
final case class RejectNoObs(prop: Proposal) extends RejectMessage {
  def reason: String = RejectNoObs.name
  def detail: String = RejectNoObs.description
}