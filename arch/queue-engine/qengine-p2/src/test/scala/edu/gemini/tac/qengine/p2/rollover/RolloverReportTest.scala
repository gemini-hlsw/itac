package edu.gemini.tac.qengine.p2.rollover

import org.junit._
import Assert._

import edu.gemini.tac.qengine.ctx.Site

class RolloverReportTest {
  import RolloverFixture._

  val report = RolloverReport(List(obsNormal, obsSpaces, obsGN, obsAnyConds))

  @Test def testSiteFilter() {
    val gs     = report.filter(Site.south)
    assertEquals(List(obsNormal, obsSpaces, obsAnyConds), gs.obsList)

    val gn     = report.filter(Site.north)
    assertEquals(List(obsGN), gn.obsList)

    val empty  = gn.filter(Site.south)
    assertEquals(Nil, empty.obsList)

    assertEquals(Nil, RolloverReport.empty.filter(Site.south).obsList)
  }

  @Test def testTimeSum() {
    assertEquals( 0.0, RolloverReport.empty.total.toHours.value, 0.000001)
    assertEquals(10.0, report.total.toHours.value, 0.000001)
  }
}
