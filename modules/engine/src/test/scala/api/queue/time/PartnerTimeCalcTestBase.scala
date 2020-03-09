package edu.gemini.tac.qengine.api.queue.time

import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p1.{CoreProposal, Mode, Ntac}
import edu.gemini.spModel.core.Site

/**
 * Base trait for PartnerTimeCalc tests.
 */
trait PartnerTimeCalcTestBase {

  protected def assertZero(lst: List[Partner], pt: PartnerTimes): Unit = {
    lst.foreach { p =>
      assertEquals(Time.ZeroHours, pt(p))
    }
  }

  protected def mkProp(p: Partner, id: String, t: Time, s: Site, m: Mode): CoreProposal = {
    val ntac = Ntac(p, id, 1.0, t)
    CoreProposal(ntac, s, m)
  }
}
