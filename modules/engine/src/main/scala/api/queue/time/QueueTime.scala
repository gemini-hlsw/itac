package edu.gemini.tac.qengine.api.queue.time


import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.util.{Percent, Time}

object QueueTime {
  /** Number of hours in each "cycle" of 100 Partner countries. */
  val CycleTimeConstant = 300
  val DefaultPartnerOverfillAllowance = Percent(5)
}

/** Implementation of `QueueTime` derived from overall partner allocation and
  * band percentages.
  */
final case class QueueTime(categorizedTimes: Map[(Partner, QueueBand), Time], val partnerOverfillAllowance: Map[QueueBand, Percent]) {

  /** Size of time quantum as
    * (partner queue time * 300) / (total queue time * partner percentage share)
    */
  def quantum(p: Partner): Time = {
    val fullQueueTimeForThisPartnerTimesOneHundred = full.toHours.value * partnerPercent(p).doubleValue
    if (fullQueueTimeForThisPartnerTimesOneHundred == 0)
      Time.ZeroHours
    else {
      val d1 = fullPartnerTime(p).toHours.value * QueueTime.CycleTimeConstant
      Time.hours(d1/fullQueueTimeForThisPartnerTimesOneHundred)
    }
  }

  /** Gets a map of Partner -> Time quantum with keys for all partners.
    */
  def partnerQuanta: PartnerTime =
    PartnerTime.fromFunction(quantum)

  val allPartners: List[Partner] =
    categorizedTimes.keys.map(_._1).toList.distinct

  def overfillAllowance(band: QueueBand): Percent = partnerOverfillAllowance.getOrElse(band, Percent.Zero)

  val bandTimes: Map[QueueBand, Time] =
    categorizedTimes.foldLeft(Map.empty[QueueBand, Time].withDefaultValue(Time.Zero)) { case (m,((_, b), t)) =>
      m.updated(b, m(b) + t)
    }

  val partnerTimes: Map[Partner, Time] =
    categorizedTimes.foldLeft(Map.empty[Partner, Time].withDefaultValue(Time.Zero)) { case (m, ((p, _), t)) =>
      m.updated(p, m(p) + t)
    }

  private def sum(filter: ((Partner, QueueBand)) => Boolean): Time =
    categorizedTimes.foldLeft(Time.Zero) { case (sum, (pb, t)) =>
      sum + (if (filter(pb)) t else Time.Zero)
    }

  val fullPartnerTime: PartnerTime =
    PartnerTime.fromMap(partnerTimes)

  val full: Time =
    sum(Function.const(true))

  private def bandFilteredPartnerTime(f: QueueBand => Boolean): PartnerTime = {
    val m = categorizedTimes.collect { case ((p, b), t) if f(b) => p -> t }.toMap
    PartnerTime.fromMap(m)
  }

  def partnerTime(band: QueueBand): PartnerTime =
    bandFilteredPartnerTime(_ == band)

  def apply(band: QueueBand, p: Partner): Time =
    categorizedTimes.getOrElse((p, band), Time.Zero)

  def partnerPercent(p: Partner): Percent =
    Percent(100.0 * fullPartnerTime(p).toHours.value / full.toHours.value)

}

