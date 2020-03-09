package edu.gemini.tac.qengine.impl.queue

import org.junit._
import Assert._
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.Site

// This test is @Ignore so there is no attempt to run it by itself.  It is run
// via the two subclasses, EagerMergeTest and LazyMergeTest.

/**
 * Merge tests involving two parts.  This is sufficient for testing the
 * eager merge, but the lazy merge results can differ from the eager merge
 * when multiple parts are involved because of the precision lost in the
 * index calculation by the stepwise merge of parts.  That is, each time two
 * parts are merged by the eager strategy, a single integral index is created.
 * The lazy merge, on the other hand, has a more accurate weighted average.
 */
@Ignore abstract class TwoPartMergeTest(val s: MergeStrategy) {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  protected def core(partner: Partner, hrs: Double): CoreProposal = {
    val ntac = Ntac(partner, "x", 0, Time.hours(hrs))
    CoreProposal(ntac, site = Site.GS)
  }

  protected def masterPart(jointId: String, partner: Partner, hrs: Double): JointProposalPart =
    JointProposalPart(jointId, core(partner, hrs))

  protected def otherPart(mp: JointProposalPart, partner: Partner, hrs: Double): JointProposalPart =
    JointProposalPart(mp.jointIdValue, mp.core.copy(ntac = Ntac(partner, "x", 0, Time.hours(hrs))))

  // Place the initial part into an empty list.
  @Test def testInitialJointEmptyList() {
    val p = masterPart("j-1", GS, 10)

    val lst = s.add(p, Nil)
    s.merge(lst) match {
      case List(j: JointProposal) => {
        assertEquals("j-1", j.jointId.get)
        assertEquals(Time.hours(10), j.ntac.awardedTime)
      }
      case _ => fail()
    }
  }

  // Add the first part to a non empty list.
  @Test def testInitialJointNonEmptyList() {
    val pBR = core(BR, 10)
    val pGS = masterPart("j-1", GS, 10)

    val lst = s.add(pGS, List(pBR))
    s.merge(lst) match {
      case List(j: JointProposal, `pBR`) => {
        assertEquals("j-1", j.jointId.get)
        assertEquals(Time.hours(10), j.ntac.awardedTime)
      }
      case _ => fail()
    }
  }

  @Test def testMergeAdjacent() {
    val pGS = masterPart("j-1", GS, 10)
    val pUS = otherPart(pGS, US, 10)

    val lst = s.add(pUS, s.add(pGS, Nil))
    s.merge(lst) match {
      case List(j: JointProposal) => {
        assertEquals("j-1", j.jointId.get)
        assertEquals(Time.hours(20), j.ntac.awardedTime)
      }
      case _ => fail()
    }
  }

  @Test def testPreferExisting() {
    // Will merge pGS and pUS and give it an index of 1 since they each have 10
    // hours.  1 is exactly the index of pBR.  The algorithm will put (pGS,pUS)
    // before pBR in this case.
    val pGS = masterPart("j-1", GS, 10)
    val pBR = core(BR, 10)
    val pUS = otherPart(pGS, US, 10)

    val lst0 = pBR :: s.add(pGS, Nil)
    val lst1 = s.add(pUS, lst0)

    // (pGS,pUS) :: pBR
    s.merge(lst1) match {
      case List(j: JointProposal, `pBR`) => {
        assertEquals("j-1", j.jointId.get)
        assertEquals(Time.hours(20), j.ntac.awardedTime)
      }
      case _ => fail()
    }
  }

  @Test def testJoinTowardsTail() {
    // Give a bit more weight to the first proposal, which should cause the
    // joint to be placed after the BR proposal.
    val pGS = masterPart("j-1", GS, 11)
    val pBR = core(BR, 10)
    val pUS = otherPart(pGS, US, 10)

    val lst0 = pBR :: s.add(pGS, Nil)
    val lst1 = s.add(pUS, lst0)

    s.merge(lst1) match {
      case List(`pBR`, j: JointProposal) => {
        assertEquals("j-1", j.jointId.get)
        assertEquals(Time.hours(21), j.ntac.awardedTime)
        assertEquals(GS, j.core.ntac.partner)
        assertEquals(GS, j.ntac.partner)
      }
      case _ => fail()
    }
  }

  @Test def testJoinTowardsHead() {
    // Give a bit more weight to the second part, which should cause the
    // joint to be placed before the BR proposal.
    val pUS = masterPart("j-1", US, 11)
    val pGS = otherPart(pUS, GS, 10)
    val pBR = core(BR, 10)

    val lst0 = pBR :: s.add(pGS, Nil)
    val lst1 = s.add(pUS, lst0)

    s.merge(lst1) match {
      case List(j: JointProposal, `pBR`) => {
        assertEquals("j-1", j.jointId.get)
        assertEquals(Time.hours(21), j.ntac.awardedTime)
        assertEquals(Set(US, GS), j.toParts.map(_.ntac.partner).toSet)
      }
      case _ => fail()
    }
  }

  private def insert_AR_into_AU_BR_CA_jGS_UH(arHrs: Double, gsHrs: Double): List[Partner] = {
    // The part to merge into the list
    val pAR = masterPart("j-1", AR, arHrs)

    // Define the AU, BR, CA, GS (Joint), UH list
    val pAU = core(AU, 1)
    val pBR = core(BR, 1)
    val pCA = core(CA, 1)
    val pGS = otherPart(pAR, GS, gsHrs)
    val pUH = core(UH, 1)

    val lst: List[Proposal] = pAU :: pBR :: pCA :: s.add(pGS, List(pUH))
    s.merge(s.add(pAR, lst)).flatMap {
      case c: CoreProposal       => List(c.ntac.partner)
      case j: JointProposal      => j.toParts.map(_.ntac.partner).sortBy(_.id)
      case _: DelegatingProposal => sys.error("Shouldn't happen")
    }
  }

  @Test def testInsert() {
    assertEquals(List(AR, GS, AU, BR, CA, UH), insert_AR_into_AU_BR_CA_jGS_UH(100, 1))
    assertEquals(List(AU, AR, GS, BR, CA, UH), insert_AR_into_AU_BR_CA_jGS_UH(16.66667, 10))
    assertEquals(List(AU, BR, AR, GS, CA, UH), insert_AR_into_AU_BR_CA_jGS_UH(6, 10))
    assertEquals(List(AU, BR, CA, AR, GS, UH), insert_AR_into_AU_BR_CA_jGS_UH(1.42857, 10))
    assertEquals(List(AU, BR, CA, AR, GS, UH), insert_AR_into_AU_BR_CA_jGS_UH(0.0000001, 1000000))
  }

  @Test def testMergeJoints() {
    val pAR  = masterPart("j-1", AR, 10)
    val pAU  = otherPart(pAR, AU, 10)
    val j1_0 = JointProposal.merge(List(pAR, pAU))

    val pBR = core(BR, 1)

    val pCA  = otherPart(pAR, CA, 10)
    val pUH  = otherPart(pAR, UH, 10)
    val j1_1 = JointProposal.merge(List(pCA, pUH))

    val lst = s.add(j1_1, s.add(pBR, s.add(j1_0, Nil)))

    // The two joints have equal time, so the preference is given to the
    // proposal that has the same index as their average.  In other words,
    // pBR is at the end of the list (which is the front of the queue)
    s.merge(lst) match {
      case List(j: JointProposal, `pBR`) => {
        assertEquals("j-1", j.jointId.get)
        assertEquals(Time.hours(40), j.ntac.awardedTime)
      }
      case _ => fail()
    }
  }
}
