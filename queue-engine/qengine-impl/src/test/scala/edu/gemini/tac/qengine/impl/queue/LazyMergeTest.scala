package edu.gemini.tac.qengine.impl.queue

import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1.JointProposal

/**
 * Test cases for the LazyMergeStategy.  A two part merge configured with the
 * lazy strategy.
 */
class LazyMergeTest extends TwoPartMergeTest(LazyMergeStrategy) {
  import edu.gemini.tac.qengine.ctx.TestPartners._

  @Test def testMultiPartMerge() {
    val pAU = masterPart("j-1", AU, 1.0)
    val pBR = core(BR, 1.0)
    val pCA = otherPart(pAU, CA, 1.0)
    val pGS = core(GS, 1.0)
    val pUH = otherPart(pAU, UH, 1.0)

    val lst = pAU :: pBR :: pCA :: pGS :: pUH :: Nil
    s.merge(lst) match {
      case List(`pBR`, j: JointProposal, `pGS`) => {
        assertEquals("j-1", j.jointId.get)
        assertEquals(Time.hours(3), j.ntac.awardedTime)
      }
      case _ => fail()
    }
  }

  // Multiple parts of multiple joints
  @Test def testMultiJointMerge() {
    val pAU = masterPart("j-1", AU,  1.0)
    val pBR = masterPart("j-2", BR,  1.0)
    val pCA = otherPart(pAU, CA, 10.0)
    val pGS = otherPart(pBR, GS, 10.0)
    val pUH = otherPart(pAU, UH,  1.0)

    val lst = pAU :: pBR :: pCA :: pGS :: pUH :: Nil

    s.merge(lst) match {
      case List(jCA: JointProposal, jGS: JointProposal) => {
        assertEquals("j-1", jCA.jointId.get)
        assertEquals(Time.hours(12), jCA.ntac.awardedTime)
        assertEquals(Set(AU,CA,UH), jCA.toParts.map(_.ntac.partner).toSet)
        assertEquals("j-2", jGS.jointId.get)
        assertEquals(Time.hours(11), jGS.ntac.awardedTime)
        assertEquals(Set(BR,GS), jGS.toParts.map(_.ntac.partner).toSet)
      }
      case _ => fail()
    }
  }
}