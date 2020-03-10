package edu.gemini.tac.qengine

import p1.{QueueBand, Observation, Proposal}
import edu.gemini.tac.qengine.util.Time

package object log {
  def rejectConditions(p: Proposal, o: Observation, b: QueueBand, c: Time, m: Time) = RejectConditions(p, o, b, c, m)
  def rejectTarget(p: Proposal, o: Observation, b: QueueBand, t: RejectTarget.RaDecType, c: Time, m: Time) = RejectTarget(p, o, b, t, c, m)
}
