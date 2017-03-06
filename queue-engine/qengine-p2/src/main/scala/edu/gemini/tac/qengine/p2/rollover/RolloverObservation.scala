package edu.gemini.tac.qengine.p2.rollover

import edu.gemini.tac.qengine.ctx.Site
import edu.gemini.tac.qengine.p2.ObservationId
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1.{CategorizedTime, ObsConditions, Target}
import edu.gemini.tac.qengine.ctx.Partner

/**
 * A class that represents a rollover time observation.  The time for each
 * rollover observation should be subtracted from the corresponding bins.
 */
case class RolloverObservation(
  partner: Partner,
  obsId: ObservationId,
  target: Target,
  conditions: ObsConditions,
  time: Time) extends CategorizedTime {

  def site: Site = obsId.site
}
