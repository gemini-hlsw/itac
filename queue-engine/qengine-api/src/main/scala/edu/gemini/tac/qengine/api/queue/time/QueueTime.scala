package edu.gemini.tac.qengine.api.queue.time


import edu.gemini.tac.qengine.api.config.QueueBandPercentages
import edu.gemini.tac.qengine.ctx.{Partner, Site}
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.p1.QueueBand._
import edu.gemini.tac.qengine.util.{Percent, Time}

import java.util.logging.{Logger, Level}

object QueueTime {
  private val Log = Logger.getLogger(this.getClass.getName)

  /** Number of hours in each "cycle" of 100 Partner countries. */
  val CycleTimeConstant = 300

  val DefaultPartnerOverfillAllowance = Percent(5)

  def apply(s: Site, m: Map[Partner, Time], partners: List[Partner]): QueueTime = new QueueTime(s, PartnerTime(partners, m))
}

import QueueTime.Log

/**
 * Record of queue times for each partner.  Provides access to the total queue
 * time and the size of the time quantum for each partner.
 */
class QueueTime(site: Site,
                val fullPartnerTime: PartnerTime,
                val bandPercentages: QueueBandPercentages = QueueBandPercentages(),
                val partnerOverfillAllowance: Option[Percent] = Some(QueueTime.DefaultPartnerOverfillAllowance)) {
  /**
   * Total time for queue observing including guaranteed time and poor weather.
   */
  val full = fullPartnerTime.total.toHours

  /** The time amount at which Band 1 scheduling ends. */
  val band1End = full * bandPercentages(QBand1)

  /** The time amount at which Band 2 scheduling ends. */
  val band2End = full * bandPercentages(Category.B1_2)

  /** The time amount at which Band 3 scheduling ends (and alias for {@link #guaranteed}). */
  val band3End = full * bandPercentages(Category.Guaranteed)

  /** The time amount at which Band 4 scheduling ends (an alias for {@link #full}). */
  def band4End = full

  /** Calculates the PartnerTime for the given queue band. */
  def partnerTime(band: QueueBand): PartnerTime = fullPartnerTime * bandPercentages(band)

  /** Calculates the PartnerTime for the given queue category. */
  def partnerTime(cat: Category): PartnerTime   = fullPartnerTime * bandPercentages(cat)


  /**
   * Time amount at which each particular queue band is defined to start and
   * end.  This will differ from the actual queue band time ranges because
   * proposals will not usually add up to exactly the amount of time available
   * in a band.  Queue band 1 always starts at zero.
   */
  def range(band: QueueBand): (Time, Time) =
    band match {
      case QBand1 => (Time.ZeroHours, band1End)
      case QBand2 => (band1End,       band2End)
      case QBand3 => (band2End,       band3End)
      case QBand4 => (band3End,       band4End)
    }

  /**
   * Gets the nominal band that corresponds to the given time according only to
   * the queue time and band percentages. In reality band 1 will usually extend
   * into part of the time which was allocated for band 2 and band 2 will extend
   * into band 3.
   */
  def band(time: Time): QueueBand =
    time match {
      case u if u < band1End  => QBand1
      case u if u < band2End  => QBand2
      case u if u < band3End  => QBand3
      case _                  => QBand4
    }

  /**
   * Computes the amount of time that is nominally designated for the given
   * partner (independent of band, category, etc).  The actual amount of time
   * will depend upon the times of the proposals in the queue.
   */
  def apply(partner: Partner): Time = fullPartnerTime(partner)

  /**
   * Computes the amount of time that is nominally designated for the given
   * queue band.  The actual amount of time per band will depend upon the
   * times of the proposals in the queue.
   */
  def apply(band: QueueBand): Time = full * bandPercentages(band)

  /**
   * Computes the amount of time that is nominally available for the given
   * queue band category.  The actual amount of time per category will depend
   * upon the times of the proposals in the queue.
   */
  def apply(cat: Category) = full * bandPercentages(cat)

  /**
   * Computes the amount of time that is nominally designated for the given
   * queue band and partner.
   */
  def apply(band: QueueBand, p: Partner): Time =
    fullPartnerTime(p) * bandPercentages(band)

  /**
   * Computes the amount of time that is nominally designated for the given
   * queue band category and partner.
   */
  def apply(cat: Category, p: Partner): Time =
    fullPartnerTime(p) * bandPercentages(cat)


  /*
  Calculates the quanta appropriate for a partner over the course of a 100-partner "cycle"
  This is not precisely equal to partner % * CycleTimeConstant because the {@link PartnerTimeCalc}
  has subtracted {@link PartnerTime}s for, e.g., classical programs.
   */
  private def quantum(p: Partner, t: Time): Time = {
    val fullQueueTimeForThisPartnerTimesOneHundred = full.toHours.value * p.percentAt(site)
    if (fullQueueTimeForThisPartnerTimesOneHundred == 0) Time.ZeroHours else {
      val d1 = t.toHours.value * QueueTime.CycleTimeConstant
      Time.hours(d1/fullQueueTimeForThisPartnerTimesOneHundred)
    }
  }

  /**
   * Size of time quantum us
   * (partner queue time * 300) / (total queue time * partner percentage share)
   */
  def quantum(p: Partner): Time = quantum(p, fullPartnerTime(p))

  /**
   * Gets a map of Partner -> Time quantum with keys for all partners.
   */
  def partnerQuanta: PartnerTime = {
    val pq = fullPartnerTime.mapTimes(quantum)
    Log.log(Level.FINE, pq.toString)
    pq
  }
}
