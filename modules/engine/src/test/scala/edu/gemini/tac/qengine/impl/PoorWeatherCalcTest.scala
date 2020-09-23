package edu.gemini.tac.qengine.impl

import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.Site
import Partner._

class PoorWeatherCalcTest {

  val PoorWeather = true
  val GoodWeather = false

  private def ntac(partner: Partner, ref: String): Ntac =
    Ntac(partner, ref, 0, Time.hours(1.0))

  private def core(partner: Partner, ref: String, poorWeather: Boolean): Proposal =
    Proposal(ntac(partner, ref), site = Site.GS, isPoorWeather = poorWeather)

  @Test def testEmpty() {
    assertEquals(Nil, PoorWeatherCalc(Nil))
  }

  @Test def testNoPoorWeather() {
    val lst = List(core(BR, "br1", GoodWeather))
    assertEquals(Nil, PoorWeatherCalc(lst))
  }

  @Test def testNonJoint() {
    val br = core(BR, "br1", GoodWeather)
    val ca = core(CA, "ca1", PoorWeather)
    val lst = List(br, ca)
    assertEquals(List(ca), PoorWeatherCalc(lst))
  }

}