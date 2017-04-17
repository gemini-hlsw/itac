package edu.gemini.tac.qengine.api.queue.time


import edu.gemini.tac.qengine.api.config.QueueBandPercentages
import edu.gemini.tac.qengine.ctx.{Partner, Site}
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.p1.QueueBand._
import edu.gemini.tac.qengine.util.{Percent, Time}

import java.util.logging.{Logger, Level}

/** Record of queue times for each partner.  Provides access to the total queue
  * time and the size of the time quantum for each partner.
  */
sealed trait QueueTime {
  def fullPartnerTime: PartnerTime

  def bandPercentages: QueueBandPercentages

  def partnerOverfillAllowance: Option[Percent]

  /** Total time for queue observing including guaranteed time and poor weather.
   */
  def full: Time

  /** The time amount at which Band 1 scheduling ends. */
  val band1End: Time

  /** The time amount at which Band 2 scheduling ends. */
  val band2End: Time

  /** The time amount at which Band 3 scheduling ends (and alias for {@link #guaranteed}). */
  val band3End: Time

  /** The time amount at which Band 4 scheduling ends (an alias for {@link #full}). */
  def band4End: Time

  /** Calculates the PartnerTime for the given queue band. */
  def partnerTime(band: QueueBand): PartnerTime

  /** Calculates the PartnerTime for the given queue category. */
  def partnerTime(cat: Category): PartnerTime

  /** Time amount at which each particular queue band is defined to start and
    * end.  This will differ from the actual queue band time ranges because
    * proposals will not usually add up to exactly the amount of time available
    * in a band.  Queue band 1 always starts at zero.
    */
  def range(band: QueueBand): (Time, Time)

  /** Gets the nominal band that corresponds to the given time according only to
    * the queue time and band percentages. In reality band 1 will usually extend
    * into part of the time which was allocated for band 2 and band 2 will extend
    * into band 3.
    */
  def band(time: Time): QueueBand

  /** Size of time quantum as
    * (partner queue time * 300) / (total queue time * partner percentage share)
    */
  def quantum(p: Partner): Time

  /** Gets a map of Partner -> Time quantum with keys for all partners.
    */
  def partnerQuanta: PartnerTime

  /** Computes the amount of time that is nominally designated for the given
    * partner (independent of band, category, etc).  The actual amount of time
    * will depend upon the times of the proposals in the queue.
    */
  def apply(partner: Partner): Time = fullPartnerTime(partner)

  /** Computes the amount of time that is nominally designated for the given
    * queue band.  The actual amount of time per band will depend upon the
    * times of the proposals in the queue.
    */
  def apply(band: QueueBand): Time

  /** Computes the amount of time that is nominally available for the given
    * queue band category.  The actual amount of time per category will depend
    * upon the times of the proposals in the queue.
    */
  def apply(cat: Category): Time

  /** Computes the amount of time that is nominally designated for the given
    * queue band and partner.
    */
  def apply(band: QueueBand, p: Partner): Time

  /** Computes the amount of time that is nominally designated for the given
    * queue band category and partner.
    */
  def apply(cat: Category, p: Partner): Time
}


object QueueTime {
  private val Log = Logger.getLogger(this.getClass.getName)

  /** Number of hours in each "cycle" of 100 Partner countries. */
  val CycleTimeConstant = 300

  val DefaultPartnerOverfillAllowance = Percent(5)

  def apply(s: Site, m: Map[Partner, Time], partners: List[Partner]): QueueTime =
    apply(s, PartnerTime(partners, m))

  def apply(s: Site, pt: PartnerTime): QueueTime =
    apply(s, pt, QueueBandPercentages.Default, Some(QueueTime.DefaultPartnerOverfillAllowance))

  def apply(s: Site, pt: PartnerTime, bp: QueueBandPercentages, poa: Option[Percent]): QueueTime =
    new DerivedQueueTime(s, pt, bp, poa)
}

import QueueTime.Log

/** Implementation of `QueueTime` derived from overall partner allocation and
  * band percentages.
  */
final class DerivedQueueTime(site: Site,
                val fullPartnerTime: PartnerTime,
                val bandPercentages: QueueBandPercentages,
                val partnerOverfillAllowance: Option[Percent]) extends QueueTime {

  override val full: Time =
    fullPartnerTime.total.toHours

  override val band1End: Time =
    full * bandPercentages(QBand1)

  override val band2End: Time =
    full * bandPercentages(Category.B1_2)

  override val band3End: Time =
    full * bandPercentages(Category.Guaranteed)

  override def band4End: Time =
    full

  override def partnerTime(band: QueueBand): PartnerTime =
    fullPartnerTime * bandPercentages(band)

  override def partnerTime(cat: Category): PartnerTime   =
    fullPartnerTime * bandPercentages(cat)

  override def range(band: QueueBand): (Time, Time) =
    band match {
      case QBand1 => (Time.ZeroHours, band1End)
      case QBand2 => (band1End,       band2End)
      case QBand3 => (band2End,       band3End)
      case QBand4 => (band3End,       band4End)
    }

  override def band(time: Time): QueueBand =
    time match {
      case u if u < band1End  => QBand1
      case u if u < band2End  => QBand2
      case u if u < band3End  => QBand3
      case _                  => QBand4
    }

  override def apply(partner: Partner): Time =
    fullPartnerTime(partner)

  override def apply(band: QueueBand): Time =
    full * bandPercentages(band)

  override def apply(cat: Category): Time =
    full * bandPercentages(cat)

  override def apply(band: QueueBand, p: Partner): Time =
    fullPartnerTime(p) * bandPercentages(band)

  override def apply(cat: Category, p: Partner): Time =
    fullPartnerTime(p) * bandPercentages(cat)

  override def quantum(p: Partner): Time = {
    val fullQueueTimeForThisPartnerTimesOneHundred = full.toHours.value * p.percentAt(site)
    if (fullQueueTimeForThisPartnerTimesOneHundred == 0)
      Time.ZeroHours
    else {
      val d1 = fullPartnerTime(p).toHours.value * QueueTime.CycleTimeConstant
      Time.hours(d1/fullQueueTimeForThisPartnerTimesOneHundred)
    }
  }

  override def partnerQuanta: PartnerTime = {
    val ps = fullPartnerTime.partners
    PartnerTime(ps, ps.map { p => p -> quantum(p) }: _*)
  }
}
