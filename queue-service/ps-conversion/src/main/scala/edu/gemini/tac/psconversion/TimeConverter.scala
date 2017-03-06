package edu.gemini.tac.psconversion

import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.persistence.phase1.{TimeUnit, TimeAmount => PsTime}

import scalaz._
import Scalaz._

/**
 * Maps time representations between persistence and queue engine formats.
 */
object TimeConverter {
  /**
   * Tries to get a Scala Int object from a java.lang.Integer.  If null, then
   * None is returned.  Otherwise, it explicitly unboxes the Integer uses an
   * implicit conversion to Scala Int.
   */
  def toInt(i: java.lang.Integer): Option[Int] = Option(i).map(_.intValue)

  /**
   * Gets a Queue Engine Time from a java.lang.Integer, if not null and
   * positive.
   */
  def toHours(i: java.lang.Integer): Option[Time] =
    for {
      t <- toInt(i)
      if t > 0
    } yield Time.hours(t)

  /**
   * Converts a Time into a PsTime matching the units as closely as possible.
   * Milliseconds are mapped to seconds (adjusting the time value appropriately).
   */
  def toPsTime(t: Time): PsTime =
    t.unit match {
      case Time.Millisecs => new PsTime(t.toMinutes.value, TimeUnit.MIN)
      case Time.Seconds   => new PsTime(t.toMinutes.value, TimeUnit.MIN)
      case Time.Minutes   => new PsTime(t.toMinutes.value, TimeUnit.MIN)
      case Time.Hours     => new PsTime(t.toHours.value, TimeUnit.HR)
      case Time.Nights    => new PsTime(t.toNights.value,  TimeUnit.NIGHT)
    }

  /**
   * Converts the given Time into a PsTime in hours.
   */
  def toPsTimeHours(t: Time): PsTime = new PsTime(t.toHours.value, TimeUnit.HR)

  def MISSING_TIME_UNITS                   = "Time object missing time units"
  def UNRECOGNIZED_TIME_UNITS(t: TimeUnit) = "Time units %s not recognized".format(t.value)

  def toTime(amt: Double, units: TimeUnit): PsError \/ Time =
    units match {
      case u if u == TimeUnit.MIN   => Time.minutes(amt).right
      case u if u == TimeUnit.HR    => Time.hours(amt).right
      case u if u == TimeUnit.NIGHT => Time.nights(amt).right
      case u                        => BadData(UNRECOGNIZED_TIME_UNITS(u)).left
    }

  /**
   * Converts the given PsTime into a Time.
   */
  def toTime(ps: PsTime): PsError \/ Time =
    for {
      amt <- nullSafePersistence("Missing time amount") { ps.getValue }
      unt <- nullSafePersistence(MISSING_TIME_UNITS) { ps.getUnits }
      t   <- toTime(amt.doubleValue, unt)
    } yield t
}