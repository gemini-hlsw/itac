package edu.gemini.tac.qengine.api.queue.time


import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.p1.QueueBand._
import edu.gemini.tac.qengine.util.{Percent, Time}

/** Record of queue times for each partner.  Provides access to the total queue
  * time and the size of the time quantum for each partner.
  */
trait QueueTime {
  def fullPartnerTime: PartnerTime

  def overfillAllowance(cat: QueueBand.Category): Option[Percent]

  /** Total time for queue observing including guaranteed time and poor weather.
   */
  def full: Time

  /** The time amount at which Band 1 scheduling ends. */
  def band1End: Time

  /** The time amount at which Band 2 scheduling ends. */
  def band2End: Time

  /** The time amount at which Band 3 scheduling ends (and alias for {@link #guaranteed}). */
  def band3End: Time

  /** The time amount at which Band 4 scheduling ends (an alias for {@link #full}). */
  def band4End: Time =
    full

  /** Calculates the PartnerTime for the given queue band. */
  def partnerTime(band: QueueBand): PartnerTime

  /** Calculates the PartnerTime for the given queue category. */
  def partnerTime(cat: Category): PartnerTime

  /** Time amount at which each particular queue band is defined to start and
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

  /** Gets the nominal band that corresponds to the given time according only to
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

  protected def partnerPercent(p: Partner): Percent

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
  def partnerQuanta: PartnerTime = {
    val ps = fullPartnerTime.partners
    PartnerTime(ps, ps.map { p => p -> quantum(p) }: _*)
  }


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
  /** Number of hours in each "cycle" of 100 Partner countries. */
  val CycleTimeConstant = 300
  val DefaultPartnerOverfillAllowance = Percent(5)
}

/** Implementation of `QueueTime` derived from overall partner allocation and
  * band percentages.
  */
final case class ExplicitQueueTime(categorizedTimes: Map[(Partner, QueueBand), Time], val partnerOverfillAllowance: Map[QueueBand.Category, Percent]) extends QueueTime {

  // For backward-compatibilty only! Just to keep all the tests from blowing up
  def this(categorizedTimes: Map[(Partner, QueueBand), Time], partnerOverfillAllowance: Option[Percent]) =
    this(
      categorizedTimes,
      partnerOverfillAllowance match {
        case None    => Map.empty[QueueBand.Category, Percent] // scala y u need type args here?
        case Some(p) => QueueBand.Category.values.map(c => c -> p).toMap
      }
    )

  val allPartners: List[Partner] =
    categorizedTimes.keys.map(_._1).toList.distinct

  def overfillAllowance(cat: QueueBand.Category): Option[Percent] =
    partnerOverfillAllowance.get(cat)

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

  override val fullPartnerTime: PartnerTime =
    PartnerTime(allPartners, partnerTimes)

  override val full: Time =
    sum(Function.const(true))

  override val band1End: Time =
    bandTimes(QBand1)

  override val band2End: Time =
    band1End + bandTimes(QBand2)

  override val band3End: Time =
    band2End + bandTimes(QBand3)

  private def bandFilteredPartnerTime(f: QueueBand => Boolean): PartnerTime = {
    val m = categorizedTimes.collect { case ((p, b), t) if f(b) => p -> t }.toMap
    PartnerTime(allPartners, m)
  }

  override def partnerTime(band: QueueBand): PartnerTime =
    bandFilteredPartnerTime(_ == band)

  override def partnerTime(cat: Category): PartnerTime   =
    bandFilteredPartnerTime(_.categories(cat))

  override def apply(partner: Partner): Time =
    fullPartnerTime(partner)

  override def apply(band: QueueBand): Time =
    bandTimes(band)

  override def apply(cat: Category): Time =
    sum { case (_, b) => b.categories(cat) }

  override def apply(band: QueueBand, p: Partner): Time =
    categorizedTimes.getOrElse((p, band), Time.Zero)

  override def apply(cat: Category, p: Partner): Time =
    sum { case (p0, b) => p == p0 && b.categories(cat) }

  def partnerPercent(p: Partner): Percent =
    Percent(100.0 * fullPartnerTime(p).toHours.value / full.toHours.value)

}

