package edu.gemini.tac.qengine.api.queue.time

import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.p1.QueueBand._
import edu.gemini.tac.qengine.ctx.{TestPartners, Partner, Site}

class QueueTimeTest {
  private val site = Site.north
  import TestPartners._

  val partners = All

  private def timeEquals(hrs: Double, t: Time) {
    assertEquals(hrs, t.toHours.value, 0.000001)
  }

  @Test def testTotalQueueTime() {
    val m = Map[Partner, Time](US -> Time.hours(100), BR -> Time.hours(20))
    val qtime = QueueTime(site, m, partners)

    timeEquals(120, qtime.full)
    timeEquals(96, qtime(Category.Guaranteed))
    timeEquals(24, qtime(Category.PoorWeather))
    timeEquals(36, qtime.band1End)

    // Undefined partners
    assertEquals(Time.Zero, qtime(Category.Guaranteed, AR))
    assertEquals(Time.Zero, qtime(Category.PoorWeather, AR))

    // Defined
    timeEquals(80, qtime(Category.Guaranteed, US))
    timeEquals(16, qtime(Category.Guaranteed, BR))
  }

  @Test def testPartnerQuanta() {
    val m = Map[Partner, Time](US -> Time.hours(100), BR -> Time.hours(20))
    val qtime = QueueTime(site, m, partners)

    // Creates zero time for non-defined time quanta.
    timeEquals(0, qtime.quantum(AR))
    timeEquals(0, qtime.partnerQuanta(AR))

    timeEquals((100 * 300) / (120 * US.percentAt(site)), qtime.quantum(US))

    // BR = (20 * 300) / (120 * 5)
    timeEquals((20 * 300) / (120 * BR.percentAt(site)), qtime.quantum(BR))
  }

  private def verifyTimes(lst: List[Time], f: QueueBand => Time) {
    QueueBand.values.zip(lst).foreach {
      case (band, time) => timeEquals(time.toHours.value, f(band))
    }
  }

  private def verifyTimes(b1: Int, b2: Int, b3: Int, f: QueueBand => Time) {
    verifyTimes(List(Time.hours(b1), Time.hours(b2), Time.hours(b3)), f)
  }

  @Test def testBandTimes() {
    val m = Map[Partner, Time](US -> Time.hours(100))
    val qtime = QueueTime(site, m, partners)

    // Test nominal time ranges.
    val nominalTimeRanges = List(
      (Time.ZeroHours, Time.hours(30)),
      (Time.hours(30), Time.hours(60)),
      (Time.hours(60), Time.hours(80))
    )
    QueueBand.values.zip(nominalTimeRanges).foreach {
      case (band, range) => assertEquals(range, qtime.range(band))
    }

    // Test nominal band times.
    verifyTimes(30, 30, 20, qtime.apply)
  }

  @Test def testBandAtTime() {
    val m = Map[Partner, Time](US -> Time.hours(100))
    val qtime = QueueTime(site, m, partners)

    val expected = List(
      (QBand1, Time.Zero),
      (QBand1, Time.hours(29.9)),
      (QBand2, Time.hours(30.0)),
      (QBand2, Time.hours(59.9)),
      (QBand3, Time.hours(60.0)),
      (QBand3, Time.hours(79.9)),
      (QBand4, Time.hours(80.0)),
      (QBand4, Time.hours(100.0)),
      (QBand4, Time.hours(100.1))
    )

    expected foreach {
      case (band, time) => assertEquals(band, qtime.band(time))
    }
  }
}