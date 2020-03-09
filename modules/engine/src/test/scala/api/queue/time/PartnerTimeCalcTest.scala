package edu.gemini.tac.qengine.api.queue.time

import edu.gemini.tac.qengine.api.queue.time.PartnerTimeCalc._
import edu.gemini.tac.qengine.ctx.TestPartners
import edu.gemini.tac.qengine.p1.Mode
import edu.gemini.tac.qengine.util.Time
import edu.gemini.spModel.core.Site

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
    val b = base(Site.GN, Time.hours(100), partners)
    assertEquals(Time.hours(US.percentAt(Site.GN)), b(US))
    assertEquals(Time.hours(0), b(CL)) // no CL at Site.GN
    assertEquals(Time.hours(UH.percentAt(Site.GN)), b(UH))
  }

  @Test def testSimpleNet() {
    val us = mkProp(US, "us-1", Time.hours(5), Site.GN, Mode.Classical)
    val b  = base(Site.GN, Time.hours(100), partners)
    val c  = classical(Site.GN, List(us), partners)
    val r  = PartnerTimes(US -> Time.hours(20))
    val n  = net(b, c, r)

    assertEquals(US.percentAt(Site.GN) - 5.0 - 20.0, n(US).toHours.value, delta) // 5 classical, 20 rollover
    assertEquals(AR.percentAt(Site.GN), n(AR).toHours.value, delta)              // no adjustments
  }

  @Test def testNoNegativeResult() {
    val us = mkProp(US, "us-1", Time.hours(60), Site.GN, Mode.Classical)
    val b  = base(Site.GN, Time.hours(100), partners)
    val c  = classical(Site.GN, List(us), partners)
    val r  = PartnerTimes(US -> Time.hours(30))
    val n  = net(b, c, r)

    assertEquals(Time.hours(0), n(US))                            // not negative
    assertEquals(AR.percentAt(Site.GN), n(AR).toHours.value, delta) // no adjustments
  }
}
