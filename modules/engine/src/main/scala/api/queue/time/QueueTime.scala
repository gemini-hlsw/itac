package edu.gemini.tac.qengine.api.queue.time


import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.util.{Percent, Time}

object QueueTime {
  /** Number of hours in each "cycle" of 100 Partner countries. */
  val Quantum = Time.hours(3.0)
  val DefaultPartnerOverfillAllowance = Percent(5)
}

/** Partner time plus an overfill allowance, for a paricular queue. */
final case class QueueTime(partnerTime: PartnerTime, val overfillAllowance: Percent) {

  def partnerQuanta: PartnerTime =
    PartnerTime.fromFunction(p => if (partnerTime(p) == Time.Zero) Time.Zero else QueueTime.Quantum)

  def apply(p: Partner): Time =
    partnerTime(p)

}

