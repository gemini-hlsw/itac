package edu.gemini.tac.qengine.p1

import org.junit._
import Assert._

import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.ctx.Partner
import org.mockito.Mockito
import edu.gemini.spModel.core.Site

class JointProposalTest {
  def mockPartner(partnerCountryKey: String): Partner = {
    val p = Mockito.mock(classOf[Partner])
    Mockito.when(p.id).thenReturn(partnerCountryKey)
    p
  }
  val AR = mockPartner("AR")
  val BR = mockPartner("BR")
  val CA = mockPartner("CA")
  val UH = mockPartner("UH")
  val US = mockPartner("US")
  val AU = mockPartner("AU")

  val site    = Site.GS
  val target0 = Target(0.0, 0.0) // required but not used for this test
  val conds50 = ObservingConditions(cc = CloudCover.CC50)

  def mkObs(hrs: Double): Observation = Observation(target0, conds50, Time.hours(hrs))

  def mkProp(partner: Partner, ref: String, hrs: Double, obsHrs: Double*): CoreProposal = {
    val ntac = Ntac(partner, ref, 0, Time.hours(hrs))
    val lst  = obsHrs.map(mkObs).toList
    CoreProposal(ntac, site, obsList = lst)
  }

  def ntac(partner: Partner, ref: String, hrs: Double): Ntac =
    Ntac(partner, ref, 0, Time.hours(hrs))
  def ntac(partner: Partner, ref: String): Ntac = ntac(partner, ref, 10)
  def core(partner: Partner, ref: String): CoreProposal =
    CoreProposal(ntac(partner, ref), site)

  @Test def testDelegates() {
    val m = mkProp(AR, "ar1", 10, 2, 2, 6).copy(isPoorWeather = true)
    assertTrue(m.isPoorWeather)

    val part1 = JointProposalPart("j1", m.copy(ntac = ntac(BR, "br1", 20)))
    val part2 = JointProposalPart("j1", m.copy(ntac = ntac(CA, "ca1", 30)))
    assertTrue(part1.isPoorWeather)
    assertTrue(part2.isPoorWeather)

    val j = JointProposal.merge(List(part1, part2))
    assertEquals("j1", j.jointId.get)
    assertTrue(j.isPoorWeather)
    assertEquals(3, j.obsList.size)
    assertEquals(Time.hours(2), j.obsList(0).time)
    assertEquals(Time.hours(10), Observation.relativeObsTime(j.obsList(0), j.time, j.obsList)) // (20 + 30) * 2/10
    assertEquals(Time.hours(2), j.obsList(1).time)
    assertEquals(Time.hours(10), Observation.relativeObsTime(j.obsList(1), j.time, j.obsList)) // (20 + 30) * 2/10
    assertEquals(Time.hours(6), j.obsList(2).time)
    assertEquals(Time.hours(30), Observation.relativeObsTime(j.obsList(2), j.time, j.obsList)) // (20 + 30) * 6/10
  }

  @Test def testCreatesNtacCorrectly() {
    val m     = mkProp(AR, "ar1", 10, 2, 2, 6).copy(isPoorWeather = true)
    val part1 = JointProposalPart("j1", m.copy(ntac = ntac(BR, "br1", 20)))
    val part2 = JointProposalPart("j1", m.copy(ntac = ntac(CA, "ca1", 30)))
    val j     = JointProposal.merge(List(part1, part2))

    assertEquals(BR, j.ntac.partner)
    assertEquals("j1", j.ntac.reference)
    assertEquals(0, j.ntac.ranking.num.get, 0.00001)
    assertEquals(Time.hours(50), j.ntac.awardedTime)
  }

  @Test def testContainsId() {
    val pAR = core(AR, "ar1")
    val pBR = core(BR, "br1")
    val pCA = core(CA, "ca1")
    val j   = JointProposal("j1", pAR, List(pBR.ntac, pCA.ntac))

    // JointProposal contains the ids of its parts
    assertTrue(j.containsId(pBR.id))
    assertTrue(j.containsId(pCA.id))

    // Doesn't contain the id of its master, unless it is in the parts list
    assertFalse(j.containsId(pAR.id))

    // JointProposal contains its own id
    assertTrue(j.containsId(j.id))

    // Part doesn't contain the id of its joint
    assertFalse(j.toParts.exists(_.containsId(j.id)))
  }

  @Test def testToParts() {
    val ntacAR = ntac(AR, "ar1")
    val ntacBR = ntac(BR, "br1")
    val prop   = core(AR, "ar1")
    val part1  = JointProposalPart("j1", prop)
    val part2  = JointProposalPart("j1", prop.copy(ntac = ntacBR))
    val j      = JointProposal("j1", prop, ntacAR, ntacBR)
    assertEquals(List(part1, part2), j.toParts)
  }

  @Test def testMergePartsListProducesSome() {
    val ntacBR = ntac(BR, "br1")
    val prop   = core(AR, "ar1")
    val part1  = JointProposalPart("j1", prop)
    val part2  = JointProposalPart("j1", prop.copy(ntac = ntacBR))

    val partList = List(part1, part2)

    JointProposal.mergeOption(partList) match {
      case Some(jp) =>
        assertEquals(List(AR, BR), jp.toParts.map(_.ntac.partner))
      case _ => fail
    }

    assertEquals(List(AR, BR), JointProposal.merge(partList).toParts.map(_.ntac.partner))
  }

  @Test def testMergeEmptyPartListProducesNone() {
    assertEquals(None, JointProposal.mergeOption(Nil))

    try {
      JointProposal.merge(Nil)
      fail
    } catch {
      case ex: java.util.NoSuchElementException => // ok
    }
  }

  @Test def testMergeMismatchedPartsProducesNone() {
    val prop  = core(AR, "ar1")
    val part1 = JointProposalPart("j1", prop)
    val part2 = JointProposalPart("j2", prop)

    assertEquals(None, JointProposal.mergeOption(List(part1, part2)))

    try {
      JointProposal.merge(List(part1, part2))
      fail
    } catch {
      case ex: java.util.NoSuchElementException => // ok
    }
  }

  @Test def testMergeMatchingEmptyPartListProducesEmptyList() {
    assertEquals(Nil, JointProposal.mergeMatching(Nil))

  }

  @Test def testMergeMatchingHomogeneousPartListProducesSingletonList() {
    val pBR = JointProposalPart("j1", core(BR, "br1"))
    val pCA = JointProposalPart("j1", core(CA, "ca1"))

    val expect = List(JointProposal("j1", pBR.core, pBR.ntac, pCA.ntac))

    assertEquals(expect, JointProposal.mergeMatching(List(pBR, pCA)))
  }

  @Test def testMergeMatchingHeterogeneousPartListProducesCorrespondingMultiElementList() {
    val pBR = JointProposalPart("j1", mkProp(BR, "br1", 10))
    val pCA = JointProposalPart("j1", mkProp(CA, "ca1", 10))

    val pUH = JointProposalPart("j2", mkProp(UH, "uh1", 10))
    val pUS = JointProposalPart("j2", mkProp(US, "us1", 10))

    val allParts = List(pBR, pUH, pUS, pCA)

    val jointList = JointProposal.mergeMatching(allParts)

    // Verify that we get the 3 expected joint proposals.
    assertEquals(2, jointList.size)

    val jMap = jointList.map(jp => (jp.jointIdValue -> jp)).toMap

    val j1 = jMap.get("j1").get
    assertEquals(Set(BR, CA), j1.toParts.map(_.ntac.partner).toSet)
    val j2 = jMap.get("j2").get
    assertEquals(Set(UH, US), j2.toParts.map(_.ntac.partner).toSet)
  }

  @Test def testExpandJoints() {
    val pBR = core(BR, "br1")

    val pAR = core(AR, "ar1")
    val pCA = core(CA, "ca1")
    val j1  = JointProposal("j1", pAR, pAR.ntac, pCA.ntac)

    val pUH = core(UH, "uh1")

    val pAU = core(AU, "au1")
    val pUS = core(US, "us1")
    val j2  = JointProposal("j2", pAU, pUS.ntac) // master only included if in the list of parts

    // Expanding joint proposals results in a list of proposal parts in the
    // order they are set in the joint proposal.
    val l1  = List(pBR, j1, pUH, j2)
    val l1e = List(pBR, pAR, pCA, pUH, pUS).map(_.id) // AU not in list
    val l1a = Proposal.expandJoints(l1).map(_.id)
    assertEquals(l1e, l1a)

    // Expanding a list of non joints just produces the same list.
    val l2  = List(pBR, pUH)
    val l2e = l2.map(_.id)
    val l2a = Proposal.expandJoints(l2).map(_.id)
    assertEquals(l2e, l2a)

    // Expanding an empty list produces an empty list.
    val l3 = List.empty
    assertEquals(l3, Proposal.expandJoints(l3))
  }
}
