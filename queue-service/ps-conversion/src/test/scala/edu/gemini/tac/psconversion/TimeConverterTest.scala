package edu.gemini.tac.psconversion

import org.junit._
import Assert._


import TimeConverter._
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.persistence.phase1.{TimeUnit, TimeAmount => PsTime}
import scalaz._

class TimeConverterTest {
  @Test def testSuccessfulToInt() {
    assertEquals(42, toInt(new java.lang.Integer(42)).get)
  }

  @Test def testNullToInt() {
    assertEquals(None, toInt(null))
  }

  @Test def testSuccessfulToTime() {
    assertEquals(Time.hours(42), toHours(new java.lang.Integer(42)).get)
  }

  @Test def testNullToTime() {
    assertEquals(None, toHours(null))
  }

  @Test def testNegativeToTime() {
    assertEquals(None, toHours(new java.lang.Integer(-1)))
  }

  def assertTimesMatch(exp: PsTime, act: PsTime) {
    assertEquals(exp.getUnits,  act.getUnits)
    assertEquals(exp.getValue, act.getValue)
  }

  @Test def testToPsTime() {
    val tmin = Time.minutes(42)
    val emin = new PsTime(42, TimeUnit.MIN)
    assertTimesMatch(emin, TimeConverter.toPsTime(tmin))

    val thr  = Time.hours(42)
    val ehr  = new PsTime(42, TimeUnit.HR)
    assertTimesMatch(ehr, TimeConverter.toPsTime(thr))

    val tngt = Time.nights(42)
    val engt = new PsTime(42, TimeUnit.NIGHT)
    assertTimesMatch(engt, TimeConverter.toPsTime(tngt))
  }

  @Test def testToPsHours() {
    val exp = new PsTime(42, TimeUnit.HR)

    val tlst = List(
      Time.minutes(  42 * 60),
      Time.hours(    42),
      Time.nights(   42.0/Time.HoursPerNight)
    )

    tlst.foreach {
      t => assertTimesMatch(exp, TimeConverter.toPsTimeHours(t))
    }
  }

  @Test def testToTimeWithNullUnits() {
    TimeConverter.toTime(new PsTime(1.0, null)) match {
      case -\/(BadData(msg)) => assertEquals(TimeConverter.MISSING_TIME_UNITS, msg)
      case _        => fail()
    }
  }

  @Test def testToTimeWithGoodUnits() {
    val m = Map(
      TimeUnit.MIN -> Time.Minutes,
      TimeUnit.HR   -> Time.Hours,
      TimeUnit.NIGHT  -> Time.Nights
    )

    m.keys foreach { key =>
      TimeConverter.toTime(new PsTime(1.0, key)) match {
        case \/-(t) => assertEquals(m(key).toTime(1.0), t)
        case _      => fail()
      }
    }
  }
}