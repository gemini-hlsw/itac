package edu.gemini.tac.qengine.p1

import org.junit._
import Assert._

import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.ctx.Partner
import org.mockito.Mockito
import edu.gemini.spModel.core.Site

class JointProposalPartTest {
//  private def mockPartnerPercentage(partnerCountryKey : String, proportion : Double, queue : PsQueue, isNorth : Boolean, isSouth : Boolean) : PartnerPercentage = {
//    val partner = Mockito.mock(classOf[PsPartner])
//    Mockito.when(partner.getPartnerCountryKey).thenReturn(partnerCountryKey)
//    Mockito.when(partner.isNorth).thenReturn(isNorth)
//    Mockito.when(partner.isSouth).thenReturn(isSouth)
//
//
//    new PartnerPercentage(queue, partner, proportion)
//  }
//
//  val mockSouth = Mockito.mock(classOf[PsSite])
//  Mockito.when(mockSouth.getDisplayName).thenReturn("South")
//
//  val queue = Mockito.mock(classOf[PsQueue])
//  Mockito.when(queue.getSite).thenReturn(mockSouth)
//
//  val mockBrPP = mockPartnerPercentage("BR", 0.10, queue, true, true)
//  val BR = mockBrPP.partner
//
//  val mockARPP = mockPartnerPercentage("AR", 0.10, queue, true, true)
//  val AR  = mockARPP.partner
//
//  val mockCAPP = mockPartnerPercentage("CA", 0.19, queue, true, true)
//  val CA = mockCAPP.partner
//
//  val mockPartnerPercentages : java.util.Set[PartnerPercentage] = {
//    val mockPcts = new java.util.HashSet[PartnerPercentage]()
//    mockPcts.add(mockPartnerPercentage("CL", 0.10, queue, false, true))
//    mockPcts.add(mockARPP)
//    mockPcts.add(mockPartnerPercentage("UH", 0.05, queue, true, false))
//    mockPcts.add(mockBrPP)
//    mockPcts.add(mockCAPP)
//
//    mockPcts
//  }
//
//  Mockito.when(queue.getPartnerPercentages).thenReturn(mockPartnerPercentages)
  val BR = Mockito.mock(classOf[Partner])
  val CA = Mockito.mock(classOf[Partner])
  val AR = Mockito.mock(classOf[Partner])

  val site    = Site.GS
  val target0 = Target(0.0, 0.0) // required but not used for this test
  val conds50 = ObservingConditions(cc = CloudCover.CC50)
  val conds80 = ObservingConditions(cc = CloudCover.CC80)

  def mkObs(hrs: Double): Observation = Observation(target0, conds50, Time.hours(hrs))

  def mkProp(partner: Partner, ref: String, hrs: Double, obsHrs: Double*): CoreProposal = {
    val ntac = Ntac(partner, ref, 0, Time.hours(hrs))
    val lst  = obsHrs.map(mkObs).toList
    CoreProposal(ntac, site, obsList = lst)
  }

  def mkProp(partner: Partner, ref: String): CoreProposal = {
    val ntac = Ntac(partner, ref, 0, Time.hours(10))
    CoreProposal(ntac, site)
  }

  private def assertIsDefaultSettings(p: Proposal) {
    assertEquals(site, p.site)
    assertEquals(Mode.Queue, p.mode)
    assertEquals(Too.none, p.too)
    assertFalse(p.isPoorWeather)
  }

  // Uses the contained proposal to extract NTAC information, but the master
  // proposal for all other details.
  @Test def testDelegates() {
    val c = mkProp(BR, "br1", 20, 2, 2, 2, 2, 2).copy(
      site = Site.GN,
      mode = Mode.Classical,
      too = Too.rapid,
      band3Observations = List(
        Observation(target0, conds80, Time.hours(1)),
        Observation(target0, conds80, Time.hours(1)),
        Observation(target0, conds80, Time.hours(1))
      ),
      isPoorWeather = true
    )
    assertEquals(Too.rapid, c.too)

    val jp = JointProposalPart("j1", c)

    assertEquals(Ntac(BR, "br1", 0, Time.hours(20)), jp.ntac)
    assertEquals(Time.hours(20), jp.time)
    assertEquals(jp.obsList.size, 5)
    assertEquals(jp.band3Observations.size, 3)
    assertEquals(Time.hours(4), Observation.relativeObsTime(jp.obsList(0), jp.time, jp.obsList)) // 20 / 5
    assertEquals(Site.GN, jp.site)
    assertEquals(Mode.Classical, jp.mode)
    assertEquals(Too.rapid, jp.too)
    assertTrue(jp.isPoorWeather)
  }

  // Tests that two compatible parts are mergeable
  @Test def testMergeableWith() {
    val part1 = JointProposalPart("j1", mkProp(BR, "br1"))
    val part2 = JointProposalPart("j1", mkProp(CA, "ca1"))

    assertTrue(part1.isMergeableWith(part2))
    assertTrue(part2.isMergeableWith(part1))
  }

  // Tests that two incompatible parts are not mergeable
  @Test def testNotMergeableWithDifferingJointId() {
    val part1 = JointProposalPart("j1", mkProp(BR, "br1"))
    val part2 = JointProposalPart("j2", mkProp(CA, "ca1"))

    assertFalse(part1.isMergeableWith(part2))
    assertFalse(part2.isMergeableWith(part1))
  }

  @Test def testEmptyListOfPartsIsNotMergeable() {
    assertFalse(JointProposalPart.isMergeable(Nil))
  }

  // A list containing just one part is "mergeable".
  @Test def testSingletonListOfPartsIsMergeable() {
    val part = JointProposalPart("j1", mkProp(BR, "br1"))
    assertTrue(JointProposalPart.isMergeable(List(part)))
  }

  // A list of compatible mergeable parts is mergeable
  @Test def testCompatibleListOfPartsIsMergeable() {
    val part1 = JointProposalPart("j1", mkProp(BR, "br1"))
    val part2 = JointProposalPart("j1", mkProp(CA, "ca1"))
    assertTrue(JointProposalPart.isMergeable(List(part1, part2)))
  }

  // A list of incompatible parts is not mergeable.
  @Test def testIncompatibleListOfPartsIsNotMergeable() {
    val part1 = JointProposalPart("j1", mkProp(BR, "br1"))
    val part2 = JointProposalPart("j2", mkProp(CA, "ca1"))
    assertFalse(JointProposalPart.isMergeable(List(part1, part2)))
  }

  @Test def testToJoint() {
    val c    = mkProp(AR, "ar1")
    val part = JointProposalPart("j1", c)

    val joint = part.toJoint

    assertEquals(Some("j1"), joint.jointId)
    assertEquals(List(Ntac(AR, "ar1", 0, Time.hours(10))), joint.ntacs)
  }

  @Test def testJointProposalPartContainsId() {
    val c    = mkProp(AR, "ar1")
    val part = JointProposalPart("j1", c)
    assertTrue(part.containsId(Proposal.Id(AR, "ar1")))
  }
}
