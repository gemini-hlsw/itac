package edu.gemini.tac.qengine.p1

import org.junit._
import Assert._

import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.ctx.{Site, Partner}

class NtacTest {

  def mockPartner(partnerCountryKey : String, pct : Double) : Partner =
    Partner(partnerCountryKey, "", pct, Set(Site.south, Site.north))

  val US = mockPartner("US", 0.42)
  val AR = mockPartner("AR", 0.10)
  val UH = mockPartner("UH", 0.10)
  val CL = mockPartner("CL", 0.10)

  @Test def testRequirePositiveRanking() {
    try {
      Ntac(US, "us1", -5.0, Time.hours(10))
      fail()
    } catch {
      case ex: IllegalArgumentException => // ok
    }
  }

  @Test def testRequirePositiveTime() {
    try {
      Ntac(US, "us1", 5.0, -Time.hours(10))
      fail()
    } catch {
      case ex: IllegalArgumentException => // ok
    }
  }

  @Test def testReverseAwardedTimeOrdering() {
    val t0 = Ntac(US, "us1", 99.0, Time.hours(100.0))
    val t1 = Ntac(AR, "ar1",  1.0, Time.hours(  1.0))
    assertTrue(t0 < t1)
  }

  @Test def testPartnerPercentageOrdering() {
    val t0 = Ntac(AR, "b", 2.0, Time.hours(100.0))
    val t1 = Ntac(US, "a", 1.0, Time.hours(100.0))
    assertTrue(t0 < t1)
  }

  @Test def testRankingOrdering() {
    val t0 = Ntac(UH, "b", 1.0, Time.hours(100.0))
    val t1 = Ntac(CL, "a", 2.0, Time.hours(100.0))
    assertTrue(t0 < t1)
  }

  @Test def testUnassignedRankOrdering() {
    val r1 = Ntac.Rank(None)
    val r2 = Ntac.Rank(Some(99.0))
    val r3 = Ntac.Rank.empty

    assertTrue(r2 < r1)
    assertTrue(r1 == r3)
  }

  @Test def testUnassignedRankOrderingOfNtacs() {
    val t0 = Ntac(UH, "b", 99.0, Time.hours(100.0))
    val t1 = Ntac(CL, "a", Ntac.Rank.empty, Time.hours(100.0))
    assertTrue(t0 < t1)
  }

  @Test def testPartnerOrdering() {
    val t0 = Ntac(CL, "b", 2.0, Time.hours(100.0))
    val t1 = Ntac(UH, "a", 2.0, Time.hours(100.0))
    assertTrue(t0 < t1)
  }

  @Test def testReferenceOrdering() {
    val t0 = Ntac(CL, "a", 2.0, Time.hours(100.0))
    val t1 = Ntac(CL, "b", 2.0, Time.hours(100.0))
    assertTrue(t0 < t1)
  }

//  @Test def compareEquals() {
//    val t0 = Ntac(CL, "a", 2.0, Time.hours(100.0))
//    val t1 = Ntac(CL, "a", 2.0, Time.hours(100.0))
//    assertTrue(t0.compare(t1) == 0)
//  }
}