package edu.gemini.tac.qengine.api.queue.time

import edu.gemini.tac.qengine.api.queue.time.PartnerTimeCalc._
import edu.gemini.tac.qengine.ctx.TestPartners
import edu.gemini.tac.qengine.p1.{JointProposal, Mode}
import edu.gemini.tac.qengine.util.Time

import org.junit._
import Assert._
import edu.gemini.spModel.core.Site

class ClassicalTimeTest extends PartnerTimeCalcTestBase {
  import TestPartners._
  val partners = All

  @Test def testClassicalNoProps() {
    val c = classical(Site.GN, Nil, partners)
    assertZero(partners, c)
  }

  @Test def testSingleClassical() {
    val prop = mkProp(AR, "ar-1", Time.hours(10), Site.GN, Mode.Classical)
    val c    = classical(Site.GN, List(prop), partners)

    assertEquals(Time.hours(10), c(AR))
    assertZero(partners.filter(_ != AR), c)
  }

  @Test def testFilterSouth() {
    val ar = mkProp(AR, "ar-1", Time.hours(10), Site.GN, Mode.Classical)
    val au = mkProp(AU, "au-1", Time.hours(20), Site.GS, Mode.Classical)
    val c  = classical(Site.GN, List(ar, au), partners)

    assertEquals(Time.hours(10), c(AR))
    assertZero(partners.filter(_ != AR), c)
  }

  @Test def testFilterQueue() {
    val ar = mkProp(AR, "ar-1", Time.hours(10), Site.GN, Mode.Classical)
    val au = mkProp(AU, "au-1", Time.hours(20), Site.GN, Mode.Queue)
    val c  = classical(Site.GN, List(ar, au), partners)

    assertEquals(Time.hours(10), c(AR))
    assertZero(partners.filter(_ != AR), c)
  }

  @Test def testSumClassical() {
    val ar1 = mkProp(AR, "ar-1", Time.hours(5), Site.GN, Mode.Classical)
    val ar2 = mkProp(AR, "ar-2", Time.hours(5), Site.GN, Mode.Classical)
    val c   = classical(Site.GN, List(ar1, ar2), partners)

    assertEquals(Time.hours(10), c(AR)) // 5 + 5
    assertZero(partners.filter(_ != AR), c)
  }

  // Redundant I suppose.  Tests everything above in one case
  @Test def testClassicalAll() {
    val ar1 = mkProp(AR, "ar-1", Time.hours(5), Site.GN, Mode.Classical)
    val ar2 = mkProp(AR, "ar-2", Time.hours(5), Site.GN, Mode.Classical)
    val au  = mkProp(AU, "au-1", Time.hours(5), Site.GS, Mode.Classical) // south
    val br  = mkProp(BR, "br-1", Time.hours(5), Site.GN, Mode.Queue) // queue
    val ca  = mkProp(CA, "ca-1", Time.hours(20), Site.GN, Mode.Classical)
    val c   = classical(Site.GN, List(ar1, ar2, au, br, ca), partners)

    assertEquals(Time.hours(10), c(AR)) // 5 + 5
    assertEquals(Time.hours(20), c(CA))
    assertZero(partners.filter(p => (p != AR) && (p != CA)), c)
  }

  @Test def testJointClassical() {
    val cl1 = mkProp(CL, "cl-1", Time.hours(20), Site.GS, Mode.Classical)
    val us1 = mkProp(US, "us-2", Time.hours(3), Site.GS, Mode.Classical)
    val j   = JointProposal("j1", us1, List(us1.ntac, cl1.ntac))
    val c   = classical(Site.GS, List(j), partners)

    assertEquals(Time.hours(3), c(US))
    assertEquals(Time.hours(20), c(CL))
    assertZero(partners.filter(p => (p != CL) && (p != US)), c)
  }
}
