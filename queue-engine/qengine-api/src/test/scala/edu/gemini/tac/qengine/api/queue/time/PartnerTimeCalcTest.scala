package edu.gemini.tac.qengine.api.queue.time

import edu.gemini.tac.qengine.api.queue.time.PartnerTimeCalc._
import edu.gemini.tac.qengine.ctx.{TestPartners, Site}
import edu.gemini.tac.qengine.ctx.Site.north
import edu.gemini.tac.qengine.p1.Mode
import edu.gemini.tac.qengine.util.Time

import org.junit._
import Assert._


/**
 * Test cases for base and net partner time calculations.
 */
class PartnerTimeCalcTest extends PartnerTimeCalcTestBase {
  import TestPartners._
  val partners = All

  // Note, by choosing 100.0 hours for the available time, the net time for
  // each partner is equal to its partner percentage.

  val delta = 0.000001

  @Test def testBaseTime() {
    val b = base(north, Time.hours(100),  partners)
    assertEquals(Time.hours(US.percentAt(north)), b(US))
    assertEquals(Time.hours(0), b(CL)) // no CL at GN
    assertEquals(Time.hours(UH.percentAt(north)), b(UH))
  }

  @Test def testSimpleNet() {
    val us = mkProp(US, "us-1", Time.hours(5), north, Mode.Classical)
    val b = base(north, Time.hours(100), partners)
    val c = classical(north, List(us), partners)
    val r = PartnerTime(partners, US -> Time.hours(20))
    val n = net(b, partners, c, r)

    assertEquals(US.percentAt(north) - 5.0 - 20.0, n(US).toHours.value, delta) // 5 classical, 20 rollover
    assertEquals(AR.percentAt(north), n(AR).toHours.value, delta) // no adjustments
  }

  @Test def testNoNegativeResult() {
    val us = mkProp(US, "us-1", Time.hours(60), north, Mode.Classical)
    val b = base(Site.north, Time.hours(100), partners)
    val c = classical(north, List(us), partners)
    val r = PartnerTime(partners, US -> Time.hours(30))
    val n = net(b, partners, c, r)

    assertEquals(Time.hours(0), n(US)) // not negative
    assertEquals(AR.percentAt(north), n(AR).toHours.value, delta) // no adjustments
  }
}