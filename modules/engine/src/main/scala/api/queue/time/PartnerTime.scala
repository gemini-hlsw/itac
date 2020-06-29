package edu.gemini.tac.qengine.api.queue.time

import edu.gemini.tac.qengine.util.{Percent, Time}
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.Site
import org.slf4j.LoggerFactory

/**
 * Wraps a map Partner -> Time with some convenience.  Guarantees that the
 * map is complete, adding zero Time entries for partners not specified if
 * necessary and allows some math ops on the PartnerTime as a whole.
 */
class PartnerTime private(val partners: List[Partner], val map: Map[Partner, Time]) {

  /**
   * Total of all contained time values.
   */
  lazy val total: Time = map.foldLeft(Time.ZeroHours)(_ + _._2)

  private def timeOp(f: (Time, Time) => Time, that: PartnerTime): PartnerTime =
    mapTimes((p, t) => f(t, that(p)))

  private def percOp(f: (Time, Percent) => Time, perc: Percent): PartnerTime =
    mapTimes((_, t) => f(t, perc))

  def -(that: PartnerTime): PartnerTime = timeOp(_ - _, that)

  def filter(f: Partner => Boolean): PartnerTime = new PartnerTime(partners, map.filterKeys(f))

  def +(that: PartnerTime): PartnerTime = timeOp(_ + _, that)

  def *(perc: Percent): PartnerTime = percOp(_ * _, perc)

  def mapOrElseZero(p : Partner) : Time = map.getOrElse(p, Time.ZeroHours)

  /**
   * Lookup the Time value associated with the given Partner.  PartnerTime
   * is guaranteed to contain an entry for all Partners.
   */
  def apply(p: Partner): Time = mapOrElseZero(p)

  /**
   * Applies the given function from (Partner, Time) -> Time on every entry in
   * the PartnerTime object, returning a new PartnerTime containing the results.
   */
  def mapTimes(f: (Partner, Time) => Time): PartnerTime =
    new PartnerTime(partners, Partner.mkMap(partners, p => f(p, mapOrElseZero(p))))

  /**
   * Adds the given amount of time associated with the given partner returning
   * and updated PartnerTime to reflect the change.
   */
  def add(p: Partner, t: Time): PartnerTime =
    new PartnerTime(partners, map.updated(p, map.get(p).getOrElse(Time.ZeroHours) + t))

  override def toString = map.toString()
}

object PartnerTime {
  val LOGGER = LoggerFactory.getLogger("edu.gemini.itac")

  /**
   * Creates a PartnerTime object using the given partial function to map from
   * Partner to the associated Time value.  Any Partners for which pf is not
   * defined are set to zero hours.
   */
  def apply(partners: List[Partner],  pf: PartialFunction[Partner, Time]): PartnerTime =
    new PartnerTime(partners, Partner.mkMap(partners, pf, Time.ZeroHours))

  /**
   * Creates a PartnerTime object using the given (Partner, Time) pairs,
   */
  def apply(partners: List[Partner], tups: (Partner, Time)*): PartnerTime = apply(partners, Map(tups: _*))

  /**
   * Calculates a PartnerTime using the given function Partner -> Time
   */
  def calc(participants: List[Partner], f: Partner => Time): PartnerTime = new PartnerTime(participants, Partner.mkMap(participants, f))

  /**
   * Creates a PartnerTime object with a constant Time value for all partners.
   */
  def constant(t: Time, participants: List[Partner]): PartnerTime = calc(participants, _ => t)

  /**
   * Creates a PartnerTime object by distributing the given time amount across
   * all partners according to their relative time share.
   */
  def distribute(total: Time, site: Site, partners: List[Partner]): PartnerTime = {
    val timeByPartner = partners.map { p =>
      p -> Time.hours(total.toHours.value * p.percentDoubleAt(site) / 100.0)
    }.toMap
    LOGGER.debug("PartnerTime.distribute: " + timeByPartner)
    new PartnerTime(partners, timeByPartner)
  }

  /**
   * Creates a PartnerTime with zero hours for all partners.
   */
  def empty(partners: List[Partner]) = constant(Time.ZeroHours, partners)
}