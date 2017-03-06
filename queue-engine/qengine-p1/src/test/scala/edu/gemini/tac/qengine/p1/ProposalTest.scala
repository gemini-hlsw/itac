package edu.gemini.tac.qengine.p1

import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.{Percent, Time}
import edu.gemini.tac.qengine.ctx.{Share, Partner, Site}
import org.mockito.{Matchers, Mockito}

class ProposalTest {
   def mockPartner(partnerCountryKey : String, proportion : Double) : Partner = {
    val p = Mockito.mock(classOf[Partner])
    Mockito.when(p.id).thenReturn(partnerCountryKey)
    Mockito.when(p.share).thenReturn(Percent(proportion * 100.0))
    Mockito.when(p.percentAt(Matchers.anyObject())).thenReturn(proportion)
    p
  }

  val AR = mockPartner("AR", .02)
  val BR = mockPartner("BR", 0.10)
  val CA = mockPartner("CA",  0.19)
  val US = mockPartner("US", 0.42)

  val target = Target(0.0, 0.0) // required but not used for this test
  val conds  = ObsConditions(cc = CloudCover.CC50)
  val e      = 0.000001

  def mkObs(hrs: Double): Observation = Observation(target, conds, Time.hours(hrs))

  def mkProp(hrs: Double, obsHrs: Double*): CoreProposal = {
    val ntac = Ntac(AR, "na", 0, Time.hours(hrs))
    val lst  = obsHrs.map(curHrs => Observation(target, conds, Time.hours(curHrs))).toList
    CoreProposal(ntac, site = Site.south, obsList = lst)
  }

  @Test def testObsConditionsDifferForBand3() {
    val proto = mkProp(10, 10) // times are irrelevant for this test
    val prop  = proto.copy(band3Observations = List(Observation(target, ObsConditions.AnyConditions, Time.hours(10))))

    prop.obsList.map(o => assertEquals(conds, o.conditions))
    assertEquals(1, prop.band3Observations.size)
    prop.band3Observations.map(o => assertEquals(ObsConditions.AnyConditions, o.conditions))

  }

  @Test def testNormalProposalTime() {
    val prop = mkProp(10, 10)
    assertEquals(10.0, prop.time.value, e)
  }

  @Test def testNormalBand1RelativeTime() {
    val prop = mkProp(10, 5, 5)
    assertEquals(10.0, prop.sumObsTime(QueueBand.QBand1).toHours.value, e)
    prop.obsList.foreach(obs => {
      assertEquals(5.0, Observation.relativeObsTime(obs, prop.time, prop.obsList).value, e)
    })
  }

  private def relativeTimeList(prop: Proposal): List[Double] =
    Observation.relativeObsList(prop.time, prop.obsList) map { obs => obs.time.toHours.value }

  private def assertRelativeTimes(prop: Proposal, hrs: Double*) {
    relativeTimeList(prop).zipAll(hrs, 0.0, 0.0) foreach {
      case (actualHrs, expectedHrs) => assertEquals(expectedHrs, actualHrs, 0.000001)
    }
  }

  @Test def testNormalDistinctRelativeTime() {
    val prop = mkProp(10, 5, 1, 4)
    assertEquals(10.0, Observation.sumObsTime(prop.obsList).toHours.value, e)

    assertEquals(5.0, Observation.relativeObsTime(prop.obsList.head, prop.time, prop.obsList).value, e)
    assertEquals(1.0, Observation.relativeObsTime(prop.obsList.tail.head, prop.time, prop.obsList).value, e)
    assertEquals(4.0, Observation.relativeObsTime(prop.obsList.tail.tail.head, prop.time, prop.obsList).value, e)

    assertRelativeTimes(prop, 5, 1, 4)
  }


  @Test def testUnderAllocatedRelativeTime() {
    val prop = mkProp(10, 5, 5, 5, 5)
    assertEquals(20.0, Observation.sumObsTime(prop.obsList).toHours.value, e)
    assertEquals(2.5, Observation.relativeObsTime(prop.obsList.head, prop.time, prop.obsList).value, e)
    assertRelativeTimes(prop, 2.5, 2.5, 2.5, 2.5)
  }

  @Test def testOverAllocatedRelativeTimes() {
    val prop = mkProp(15, 5)
    assertEquals(5.0, Observation.sumObsTime(prop.obsList).toHours.value, e)
    assertEquals(15.0, Observation.relativeObsTime(prop.obsList.head, prop.time, prop.obsList).value, e)
    assertRelativeTimes(prop, 15.0)
  }

  def core(partner: Partner, id: String): CoreProposal =
    CoreProposal(Ntac(partner, id, 0, Time.Zero), site = Site.south)

  @Test def testNonJointContainsId() {
    val pBR = core(BR, "br1")

    // Normal non-joint contains its own id
    assertTrue(pBR.containsId(Proposal.Id(BR, "br1")))

    // Normal non-joint id composed of partner AND reference
    assertFalse(pBR.containsId(Proposal.Id(AR, "br1")))
    assertFalse(pBR.containsId(Proposal.Id(BR, "br2")))
  }

  @Test def testOrderingByAwardedTimeOnly() {
    val prop10 = mkProp(10)
    val prop20 = mkProp(20)
    val prop30 = mkProp(30)
    val prop40 = mkProp(40)

    val lst = List(prop20, prop40, prop10, prop30)
    val exp = List(prop40, prop30, prop20, prop10)
    assertEquals(exp, lst.sorted(Proposal.MasterOrdering))
  }

  @Test def testOrderingByPartnerPercentageOnly() {
    val prop10 = mkProp(10)

    val propAR = prop10.copy(prop10.ntac.copy(partner = AR))  //  2
    val propBR = prop10.copy(prop10.ntac.copy(partner = BR))  //  4
    val propCA = prop10.copy(prop10.ntac.copy(partner = CA))  // 12
    val propUS = prop10.copy(prop10.ntac.copy(partner = US))  // 40

    val lst = List(propBR, propUS, propCA, propAR)
    val exp = List(propAR, propBR, propCA, propUS)
    assertEquals(exp, lst.sorted(Proposal.MasterOrdering))
  }

  @Test def testOrdering() {
    val prop = mkProp(10)

    val propAR = prop.copy(prop.ntac.copy(partner = AR, awardedTime = Time.hours(10)))  //  2
    val propBR = prop.copy(prop.ntac.copy(partner = BR, awardedTime = Time.hours(30)))  //  4
    val propCA = prop.copy(prop.ntac.copy(partner = CA, awardedTime = Time.hours(30)))  // 12
    val propUS = prop.copy(prop.ntac.copy(partner = US, awardedTime = Time.hours(10)))  // 40

    val lst = List(propBR, propUS, propCA, propAR)
    val exp = List(propBR, propCA, propAR, propUS)
    assertEquals(exp, lst.sorted(Proposal.MasterOrdering))
  }

}