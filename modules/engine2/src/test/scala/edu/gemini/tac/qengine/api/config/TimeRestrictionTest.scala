package edu.gemini.tac.qengine.api.config

import org.junit._
import Assert._
import edu.gemini.tac.qengine.p1.CloudCover.CCAny
import edu.gemini.tac.qengine.p1.ImageQuality.IQAny
import edu.gemini.tac.qengine.p1.SkyBackground.SBAny
import edu.gemini.tac.qengine.p1.WaterVapor._
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.util.{Time, Percent}
import edu.gemini.tac.qengine.ctx.{TestPartners, Site}

class TimeRestrictionTest {

  val US = TestPartners.US

  private val ntac   = Ntac(US, "x", 0, Time.hours(10))
  private val target = Target(0.0, 0.0) // not used
  private def conds(wv: WaterVapor) =
    ObsConditions(CCAny, IQAny, SBAny, wv)

  private val bin = TimeRestriction("wv", Percent(10)) {
    (prop, obs, _) => obs.conditions.wv <= WV50
  }

  private def mkProp(wv: WaterVapor): Proposal =
    CoreProposal(ntac, site = Site.south, obsList = List(Observation(target, conds(wv), Time.hours(10))))


  @Test def testMatches() {
    val propList = WaterVapor.values.map(mkProp(_))
    val boolList = propList.map(prop => bin.matches(prop, prop.obsList.head, QueueBand.QBand1))
    assertEquals(List(true, true, false, false), boolList)
  }

  @Test def testUpdated() {
    assertEquals(Percent(20), bin.updated(Percent(20)).value)
  }

  @Test def testMap() {
    assertEquals(Time.hours(10), bin.map(perc => Time.hours(100) * perc).value)
  }
}