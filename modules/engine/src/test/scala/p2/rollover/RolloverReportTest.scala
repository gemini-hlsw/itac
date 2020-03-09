// package edu.gemini.tac.qengine.p2.rollover

// import org.junit._
// import Assert._
// import edu.gemini.spModel.core.Site

// class RolloverReportTest {
//   import RolloverFixture._

//   val report = RolloverReport(List(obsNormal, obsSpaces, obsGN, obsAnyConds))

//   @Test def testSiteFilter() {
//     val gs = report.filter(Site.GS)
//     assertEquals(List(obsNormal, obsSpaces, obsAnyConds), gs.obsList)

//     val gn = report.filter(Site.GN)
//     assertEquals(List(obsGN), gn.obsList)

//     val empty = gn.filter(Site.GS)
//     assertEquals(Nil, empty.obsList)

//     assertEquals(Nil, RolloverReport.empty.filter(Site.GS).obsList)
//   }

//   @Test def testTimeSum() {
//     assertEquals(0.0, RolloverReport.empty.total.toHours.value, 0.000001)
//     assertEquals(10.0, report.total.toHours.value, 0.000001)
//   }
// }
