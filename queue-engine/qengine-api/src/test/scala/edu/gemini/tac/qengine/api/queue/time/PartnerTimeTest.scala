package edu.gemini.tac.qengine.api.queue.time

import edu.gemini.tac.qengine.ctx.{TestPartners, Site}
import edu.gemini.tac.qengine.util.{Percent, Time}

import org.junit._
import Assert._

class PartnerTimeTest {
  private val site = Site.north

  import TestPartners._
  val partners = All

  val delta = 0.000001

  @Test def testComplete() {
    val pt = PartnerTime(partners, AR -> Time.hours(1), BR -> Time.hours(2))

    assertEquals(Time.hours(1), pt(AR))
    assertEquals(Time.hours(2), pt(BR))
    assertEquals(Time.hours(0), pt(US))
  }

  @Test def testCalc() {
    val pt = PartnerTime.calc(partners, p => Time.hours(p.percentAt(site)))

    assertEquals(Time.hours(US.percentAt(site)), pt(US))
    partners.foreach(p => assertEquals(Time.hours(p.percentAt(site)), pt(p)))
  }

  @Test def testConstant() {
    val t = Time.hours(10)
    val pt = PartnerTime.constant(t, partners)
    assertEquals(Time.hours(10), pt(US))
    partners.foreach(p => assertEquals(t, pt(p)))
  }

  // Selecting 100 hours to distribute makes it possible to use the partner
  // percentage as the number of expected hours.

  @Test
  def distributeBasedOnPartnerPercentages(): Unit = {
    val pt = PartnerTime.distribute(Time.hours(100), site, partners)
    assertEquals(Time.hours(US.percentAt(site)), pt(US))
    assertEquals(Time.hours(BR.percentAt(site)), pt(BR))
    partners.foreach { p =>
      val partnerPercentAtSite = p.percentAt(site)
      assertEquals("Failed for partner " + p.fullName, Time.hours(100) * Percent(partnerPercentAtSite), pt(p))
    }
  }


  @Test def testEmpty(): Unit = {
    val pt = PartnerTime.empty(partners)
    partners.foreach(p => assertEquals(Time.ZeroHours, pt(p)))
  }

  @Test
  def subtractTimeAcrossPartners(): Unit = {
    val pt1 = PartnerTime.distribute(Time.hours(100), site, partners)
    val pt2 = PartnerTime.constant(Time.hours(1), partners)
    val pt3 = pt1 - pt2

    assertEquals(Time.hours(US.percentAt(site) - 1.0), pt3(US))
    assertEquals(Time.hours(BR.percentAt(site) - 1.0), pt3(BR))

  }

  @Test def addTimeAcrossPartners(): Unit = {
    val pt1 = PartnerTime.distribute(Time.hours(100), site, partners)
    val pt2 = PartnerTime.constant(Time.hours(1), partners)
    val pt3 = pt1 + pt2

    assertEquals(Time.hours(US.percentAt(site) + 1.0), pt3(US))
    assertEquals(Time.hours(BR.percentAt(site) + 1.0), pt3(BR))
  }

  @Test def multiplyByPercentAcrossPartners(): Unit = {
    val pt1 = PartnerTime.distribute(Time.hours(100), site, partners)
    val pt2 = pt1 * Percent(10)

    assertEquals(Time.hours(US.percentAt(site) * 0.1), pt2(US))
    assertEquals(Time.hours(BR.percentAt(site) * 0.1), pt2(BR))
  }

  @Test def totalTimeIsEqualToDistributedTime(): Unit = {
    val pt1 = PartnerTime.distribute(Time.hours(100), site, partners)
    assertEquals(Time.hours(100), pt1.total)
  }

  @Test def distributesRolloverTimeEvenly(): Unit = {
    val pt = PartnerTime.distribute(Time.hours(100), site, partners)
    pt.map.map(kv => assertEquals(Time.hours(kv._1.percentAt(site)), kv._2))
  }
}