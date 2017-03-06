package edu.gemini.tac.qengine.impl

import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.ctx.{Partner, Site}

class PoorWeatherCalcTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._

  val PoorWeather = true
  val GoodWeather = false

  private def ntac(partner: Partner, ref: String): Ntac =
    Ntac(partner, ref, 0, Time.hours(1.0))

  private def core(partner: Partner, ref: String, poorWeather: Boolean): CoreProposal =
    CoreProposal(ntac(partner, ref), site = Site.south, isPoorWeather = poorWeather)

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

  @Test def testJoint() {
    val pBR = core(BR, "br1", PoorWeather)
    val br = pBR.ntac
    val gs = ntac(GS, "gs1")
    val j1 = JointProposal("j1", pBR, br, gs)

    val lst = List(j1)
    assertEquals(lst, PoorWeatherCalc(lst))
  }

  @Test def testJointParts() {
    val pBR = core(BR, "br1", PoorWeather)
    val br = pBR.ntac
    val gs = ntac(GS, "gs1")
    val j1 = JointProposal("j1", pBR, br, gs)

    val pCA = core(CA, "ca1", PoorWeather)
    val ca = pCA.ntac
    val uh = ntac(UH, "uh1")
    val j2 = JointProposal("j2", pCA, ca, uh)

    val lst = j1.toParts ::: j2.toParts

    val exp = List(j1, j2)
    assertEquals(exp, PoorWeatherCalc(lst))
  }

  @Test def testMixedJointsAndNonJoint() {
    val ar = core(AR, "ar1", GoodWeather)
    val br = core(BR, "br1", PoorWeather)
    val gs = core(GS, "gs1", GoodWeather)

    val au = core(AU, "au1", PoorWeather)
    val ca = core(CA, "ca1", PoorWeather)
    val j1 = JointProposal("j1", au, au.ntac, ca.ntac)

    val lst = List(ar, j1.toParts(0), br, j1.toParts(1), gs)
    val exp = List(j1, br)
    assertEquals(exp, PoorWeatherCalc(lst))
  }
}