package edu.gemini.tac.qengine.api.queue.time

import org.junit._
import Assert._

import edu.gemini.tac.qengine.api.queue.time.PartnerTimeCalc._
import edu.gemini.tac.qengine.ctx.Site
import edu.gemini.tac.qengine.p2.rollover.{RolloverObservation, RolloverReport}
import edu.gemini.tac.qengine.p2.ObservationId
import edu.gemini.tac.qengine.p1.{ObsConditions, Target}
import edu.gemini.tac.qengine.util.Time

class RolloverTimeTest extends PartnerTimeCalcTestBase {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  @Test def testEmptyReport() {
    val rop = RolloverReport.empty
    val partnersExceptCL = partners.filter(p => p != CL && p.id != "KECK" && p.id != "SUBARU")
    assertZero(partnersExceptCL, rollover(Site.north, rop, partners))
  }

  val partner = CA
  val obsId   = ObservationId.parse("GN-2011A-Q-1-2").get
  val target  = Target(15.0, 2.0)
  val conds   = ObsConditions.AnyConditions
  val time    = Time.hours(100.0)

  val ro      = RolloverObservation(partner, obsId, target, conds, time)

  private def assert100Even(rop: RolloverReport): Unit = {
    val pt = rollover(Site.north, rop, partners)
    assertEquals(pt.total.toHours.value, 100.0, Double.MinValue)
    partners.foreach {
      p => assertEquals("Wrong time for " + p.id , pt(p).toHours.value, p.percentDoubleAt(Site.north), Double.MinValue)
    }
  }

  @Test def testDistributeEvenly(): Unit = {
    assert100Even(new RolloverReport(List(ro)))
  }

  @Test def tstFilterSouth(): Unit = {
    val partner = BR
    val obsId   = ObservationId.parse("GS-2011A-Q-3-4").get
    val target  = Target(30.0, 3.0)
    val conds   = ObsConditions.AnyConditions
    val time    = Time.hours(6.0)

    val roSouth = RolloverObservation(partner, obsId, target, conds, time)

    val rop     = new RolloverReport(List(ro, roSouth))
    assert100Even(rop)
  }
}