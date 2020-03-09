// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.queue.time

import edu.gemini.tac.qengine.util.{Percent, Time}
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.Site

/**
 * Wraps a map Partner -> Time with some convenience.  Guarantees that the map is complete, adding
 * zero Time entries for partners not specified if necessary and allows some math ops on the
 * PartnerTime as a whole.
 */
final case class PartnerTimes(toMap: Map[Partner, Time]) extends (Partner => Time) {

  /** Total of all contained time values. */
  lazy val total: Time =
    toMap.values.foldRight(Time.ZeroHours)(_ + _)

  def -(that: PartnerTimes): PartnerTimes =
    modify((p, t) => t - that(p))

  def +(that: PartnerTimes): PartnerTimes =
    modify((p, t) => t + that(p))

  def *(perc: Percent): PartnerTimes =
    modify((_, t) => t * perc)

  /** True iff all values equal Time.Zero. */
  def isEmpty: Boolean =
    toMap.values.forall(_ == Time.Zero)

  /**
   * Lookup the Time value associated with the given Partner. PartnerTime is guaranteed to contain
   * an entry for all Partners.
   */
  def apply(p: Partner): Time =
    toMap.getOrElse(p, Time.ZeroHours)

  /**
   * Applies the given function from (Partner, Time) -> Time on every entry in the PartnerTime
   * object, returning a new PartnerTime containing the results.
   */
  def modify(f: (Partner, Time) => Time): PartnerTimes =
    copy(toMap = toMap.map { case (k, v) => (k, f(k, v)) })

  /**
   * Adds the given amount of time associated with the given partner returning and updated
   * PartnerTime to reflect the change.
   */
  def add(p: Partner, t: Time): PartnerTimes =
    copy(toMap = toMap.updated(p, apply(p) + t))

  override def toString: String =
    s"PartnerTimes($toMap)"

  override def equals(other: Any): Boolean =
    sys.error("Comparison of intensional maps doens't make sense.")

}

object PartnerTimes {

  /**
   * Creates a PartnerTime object using the given partial function to map from Partner to the
   * associated Time value.  Any Partners for which pf is not defined are set to zero hours.
   */
  def apply(partners: List[Partner], pf: PartialFunction[Partner, Time]): PartnerTimes =
    new PartnerTimes(Partner.mkMap(partners, pf, Time.ZeroHours))

  /** Creates a PartnerTime object using the given (Partner, Time) pairs, */
  def apply(tups: (Partner, Time)*): PartnerTimes =
    apply(Map(tups: _*))

  /** Calculates a PartnerTime using the given function Partner -> Time */
  def calc(participants: List[Partner], f: Partner => Time): PartnerTimes =
    new PartnerTimes(Partner.mkMap(participants, f))

  /** Creates a PartnerTime object with a constant Time value for all partners. */
  def constant(t: Time, participants: List[Partner]): PartnerTimes =
    calc(participants, _ => t)

  /**
   * Creates a PartnerTime object by distributing the given time amount across all partners
   * according to their relative time share.
   */
  def distribute(total: Time, site: Site, partners: List[Partner]): PartnerTimes = {
    val timeByPartner = partners.map { p =>
      p -> Time.hours(total.toHours.value * p.percentAt(site) / 100.0)
    }.toMap
    new PartnerTimes(timeByPartner)
  }

  /** Creates a PartnerTime with zero hours for all partners. */
  def empty: PartnerTimes =
    constant(Time.ZeroHours, Nil)

}
