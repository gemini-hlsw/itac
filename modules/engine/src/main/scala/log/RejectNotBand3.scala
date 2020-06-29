package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.Proposal

/**
 * A proposal rejection message for proposals that cannot be scheduled in
 * band 3.
 */
object RejectNotBand3 {
  val name        = "Not Band3"
  val description = "Proposal does not allow itself to be scheduled in Band 3."
}
final case class RejectNotBand3(prop: Proposal) extends RejectMessage {
  def reason: String = RejectNotBand3.name
  def detail: String = RejectNotBand3.description
}