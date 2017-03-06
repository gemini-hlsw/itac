package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.{QueueBand, Proposal}
import edu.gemini.tac.qengine.util.Percent

/**
 * A propsal rejection message for band restriction violations.
 */
object RejectBand {
  val name = "Band Restriction"

  private val reasonTemplate = "%s: %s"
  def reason(bandRestrictionName: String): String =
    reasonTemplate.format(name, bandRestrictionName)

  private val detailTemplate = "Current band: B%d. Queue time merged: %f%%."
  def detail(band: QueueBand, perc: Percent): String =
    detailTemplate.format(band.number, perc.value)
}

final case class RejectBand(prop: Proposal, restrictionName: String, band: QueueBand, perc: Percent) extends RejectMessage {
  def reason: String = RejectBand.reason(restrictionName)
  def detail: String = RejectBand.detail(band, perc)
}
