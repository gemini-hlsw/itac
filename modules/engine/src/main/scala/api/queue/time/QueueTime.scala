package edu.gemini.tac.qengine.api.queue.time


import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.util.{Percent, Time}

object QueueTime {
  /** Number of hours in each "cycle" of 100 Partner countries. */
  val Quantum = Time.hours(3.0)
  val DefaultPartnerOverfillAllowance = Percent(5)
}

/** Implementation of `QueueTime` derived from overall partner allocation and
  * band percentages.
  */
final case class QueueTime(categorizedTimes: Map[(Partner, QueueBand), Time], val partnerOverfillAllowance: Map[QueueBand, Percent]) {

  private lazy val partnerTimes: Map[Partner, Time] =
    categorizedTimes.foldLeft(Map.empty[Partner, Time].withDefaultValue(Time.Zero)) { case (m, ((p, _), t)) =>
      m.updated(p, m(p) + t)
    }

  def partnerQuanta: PartnerTime =
    PartnerTime.fromFunction(p => if (partnerTimes(p) == Time.Zero) Time.Zero else QueueTime.Quantum)

  def overfillAllowance(band: QueueBand): Percent =
    partnerOverfillAllowance.getOrElse(band, Percent.Zero)

  val fullPartnerTime: PartnerTime =
    PartnerTime.fromMap(partnerTimes)

  def apply(band: QueueBand, p: Partner): Time =
    categorizedTimes.getOrElse((p, band), Time.Zero)

}

