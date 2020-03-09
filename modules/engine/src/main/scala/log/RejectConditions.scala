// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.{QueueBand, Observation, Proposal}
import edu.gemini.tac.qengine.util.Time

/**
 * An observation rejection message for observing conditions restrictions
 * violations.
 */
object RejectConditions extends TimeBinMessageFormatter {
  val name = "Conditions Limit"

  private val detailTemplate = "%s %s"
  override def detail(
    kind: String,
    prop: Proposal,
    obs: Observation,
    band: QueueBand,
    cur: Time,
    max: Time
  ): String =
    detailTemplate.format(obs.conditions, super.detail(kind, prop, obs, band, cur, max))
}

final case class RejectConditions(
  prop: Proposal,
  obs: Observation,
  band: QueueBand,
  cur: Time,
  max: Time
) extends ObsRejectMessage {
  def reason: String = RejectConditions.name
  def detail: String = RejectConditions.detail("Conditions", prop, obs, band, cur, max)

  override def toXML = <RejectConditions>
      <RejectedProposal>{prop}</RejectedProposal>
    </RejectConditions>
}
