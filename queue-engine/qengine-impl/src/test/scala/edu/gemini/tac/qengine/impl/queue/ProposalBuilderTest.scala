package edu.gemini.tac.qengine.impl.queue

import edu.gemini.tac.qengine.api.queue.ProposalPosition
import edu.gemini.tac.qengine.api.queue.time.{PartnerTime, QueueTime}
import edu.gemini.tac.qengine.ctx.{Partner, Site}
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.QueueBand._
import edu.gemini.tac.qengine.p1.QueueBand.Category._
import CloudCover.CC50
import ImageQuality.IQ70
import SkyBackground.SB20
import WaterVapor.WV50
import edu.gemini.tac.qengine.util.{Angle, BoundedTime, Time}

import org.junit._
import Assert._

class ProposalBuilderTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  // Need obsConds, but we don't use it for this test
  private val obsConds = ObsConditions(CC50, IQ70, SB20, WV50)

  private def mkProp(partner: Partner, propTimeHours: Int, id: String): CoreProposal = {
    val ntac = Ntac(partner, id, 0, Time.hours(propTimeHours))

    // Make a proposal with just no observations.  We won't be using them anyway.
    CoreProposal(ntac, site = Site.south)
  }

  private def mkProp(propTimeHours: Int, id: String): CoreProposal =
    mkProp(GS, propTimeHours, id)

  private def mkB3Prop(propTimeHours: Int, id: String): CoreProposal = {
    val ntac = Ntac(GS, id, 0, Time.hours(propTimeHours))
    val b3 = Band3(obsConds)

    CoreProposal(ntac, site = Site.south, band3Observations = List(mkObservation))
  }

  private def mkObservation: Observation = {
    val ra = Angle.angleDeg0
    val dec = Angle.angleDeg0
    val t = new Target(ra, dec)
    val c = ObsConditions.AnyConditions
    new Observation(t, c, Time.hours(1))
  }

  private def mkBandedQueue(band1: List[Proposal], band2: List[Proposal], band3: List[Proposal], band4: List[Proposal]): Map[QueueBand, List[Proposal]] = {
    Map(QBand1 -> band1, QBand2 -> band2, QBand3 -> band3, QBand4 -> band4)
  }

  private val qs = ProposalQueueBuilder(QueueTime(Site.south, Map(GS -> Time.hours(100)), partners))

  // Expects a list of three times, one per queue band, which are matched
  // one after the other with the result of calling the function f
  private def verifyTimes(lst: List[Time], f: QueueBand => Time) {
    QueueBand.values.zip(lst).foreach {
      case (band, time) => assertEquals(time, f(band))
    }
  }

  private def verifyTimes(b1: Int, b2: Int, b3: Int, b4: Int, f: QueueBand => Time) {
    verifyTimes(List(Time.hours(b1), Time.hours(b2), Time.hours(b3), Time.hours(b4)), f)
  }

  @Test def testEmpty() {
    assertEquals(BoundedTime(Time.hours(100)), qs.bounds)
    assertEquals(BoundedTime(Time.hours(80)), qs.bounds(Guaranteed))
    assertEquals(BoundedTime(Time.hours(100)), qs.bounds(GS))
    assertEquals(BoundedTime(Time.hours(80)), qs.bounds(Guaranteed, GS))

    assertEquals(BoundedTime(Time.hours(30)), qs.bounds(QueueBand.QBand1))
    assertEquals(BoundedTime(Time.hours(30)), qs.bounds(QueueBand.QBand2))
    assertEquals(BoundedTime(Time.hours(20)), qs.bounds(QueueBand.QBand3))
    assertEquals(BoundedTime(Time.hours(20)), qs.bounds(QueueBand.QBand4))

    assertEquals(BoundedTime(Time.hours(30)), qs.bounds(QueueBand.QBand1, GS))
    assertEquals(BoundedTime(Time.hours(30)), qs.bounds(QueueBand.QBand2, GS))
    assertEquals(BoundedTime(Time.hours(20)), qs.bounds(QueueBand.QBand3, GS))
    assertEquals(BoundedTime(Time.hours(20)), qs.bounds(QueueBand.QBand4, GS))

    assertEquals(BoundedTime(Time.Zero), qs.bounds(US))
    assertEquals(BoundedTime(Time.Zero), qs.bounds(QueueBand.QBand1, US))
    assertEquals(BoundedTime(Time.Zero), qs.bounds(QueueBand.QBand2, US))
    assertEquals(BoundedTime(Time.Zero), qs.bounds(QueueBand.QBand3, US))
    assertEquals(BoundedTime(Time.Zero), qs.bounds(QueueBand.QBand4, US))

    assertEquals(Time.Zero, qs.usedTime)
    assertEquals(Time.Zero, qs.usedTime(GS))
    assertEquals(Time.Zero, qs.usedTime(QBand1))
    assertEquals(Time.Zero, qs.usedTime(Guaranteed))
    assertEquals(Time.Zero, qs.usedTime(QBand1, GS))
    assertEquals(Time.Zero, qs.usedTime(Guaranteed, GS))

    assertEquals(Time.hours(100), qs.remainingTime)
    assertEquals(Time.hours(100), qs.remainingTime(GS))
    assertEquals(Time.hours(80), qs.remainingTime(Guaranteed))
    assertEquals(Time.hours(80), qs.remainingTime(Guaranteed, GS))
    assertEquals(Time.hours(80), qs.queueTime(Guaranteed))
    assertEquals(Time.hours(100), qs.queueTime.full)

    assertEquals(Nil, qs.toList)
    assertEquals(mkBandedQueue(Nil, Nil, Nil, Nil), qs.bandedQueue)
    assertEquals(QBand1, qs.band)
    assertEquals(None, qs.positionOf(mkProp(10, "gs1")))

    assertEquals(Nil, qs.zipWithPosition)

    // Test nominal band times.
    verifyTimes(30, 30, 20, 20, qs.queueTime.apply)

    // Test actual band times for an empty queue.
    QueueBand.values.foreach {
      band =>
        assertEquals(Time.Zero, qs.usedTime(band))
        band.categories foreach {
          cat =>
            assertEquals(Time.Zero, qs.usedTime(cat))
        }
    }

    // Test remaining queue time for an empty queue.
    verifyTimes(30, 30, 20, 20, qs.remainingTime)
  }


  @Test def testAddProposal() {
    val prop = mkProp(10, "gs1")

    val qs1 = qs :+ prop

    assertEquals(Time.hours(10), qs1.usedTime)
    assertEquals(Time.hours(90), qs1.remainingTime)
    assertEquals(Time.hours(70), qs1.remainingTime(Guaranteed))

    assertEquals(List(prop), qs1.toList)
    assertEquals(mkBandedQueue(List(prop), Nil, Nil, Nil), qs1.bandedQueue)
    assertEquals(QBand1, qs1.band)
    assertEquals(ProposalPosition(qs.queueTime), qs1.positionOf(prop).get)
    assertEquals(List((prop, ProposalPosition(qs.queueTime))), qs1.zipWithPosition)

    assertEquals(Time.Zero, qs.usedTime(GS))
    assertEquals(Time.hours(100), qs.remainingTime(GS))
    assertEquals(Time.hours(80), qs.remainingTime(Guaranteed, GS))
    assertEquals(Time.hours(10), qs1.usedTime(GS))
    assertEquals(Time.hours(90), qs1.remainingTime(GS))
    assertEquals(Time.hours(70), qs1.remainingTime(Guaranteed, GS))
    assertEquals(Time.Zero, qs1.usedTime(BR))
    assertEquals(Time.Zero, qs1.remainingTime(BR))
    assertEquals(Time.Zero, qs1.remainingTime(Guaranteed, BR))

    assertEquals(BoundedTime(Time.hours(100), Time.hours(10)), qs1.bounds)
    assertEquals(BoundedTime(Time.hours(80), Time.hours(10)), qs1.bounds(Guaranteed))
    assertEquals(BoundedTime(Time.hours(100), Time.hours(10)), qs1.bounds(GS))
    assertEquals(BoundedTime(Time.hours(80), Time.hours(10)), qs1.bounds(Guaranteed, GS))

    assertEquals(BoundedTime(Time.hours(30), Time.hours(10)), qs1.bounds(QBand1))
    assertEquals(BoundedTime(Time.hours(30)), qs1.bounds(QBand2))
    assertEquals(BoundedTime(Time.hours(20)), qs1.bounds(QBand3))
    assertEquals(BoundedTime(Time.hours(30), Time.hours(10)), qs1.bounds(QBand1, GS))
    assertEquals(BoundedTime(Time.hours(30)), qs1.bounds(QBand2, GS))
    assertEquals(BoundedTime(Time.hours(20)), qs1.bounds(QBand3, GS))
    assertEquals(BoundedTime(Time.Zero), qs1.bounds(US))
    assertEquals(BoundedTime(Time.Zero), qs1.bounds(Guaranteed, US))
    assertEquals(BoundedTime(Time.Zero), qs1.bounds(QBand1, US))
    assertEquals(BoundedTime(Time.Zero), qs1.bounds(QBand2, US))
    assertEquals(BoundedTime(Time.Zero), qs1.bounds(QBand3, US))

    verifyTimes(10, 0, 0, 0, qs1.usedTime)
    verifyTimes(20, 30, 20, 20, qs1.remainingTime _)
  }

  @Test def testAddJointProposal() {
    val site = Site.south
    val qs = ProposalQueueBuilder(QueueTime(site, PartnerTime.distribute(Time.hours(100), site, partners)))
    val propGS = mkProp(10, "gs1")
    val propUS = mkProp(US, 20, "us1")
    val joint = new JointProposal("j1", propGS, List(propGS.ntac, propUS.ntac))

    val qs1 = qs :+ joint

    assertEquals(Time.hours(30), qs1.usedTime)
    assertEquals(Time.hours(70), qs1.remainingTime)

    assertEquals(Time.hours(10), qs1.usedTime(GS))
    assertEquals(Time.hours(20), qs1.usedTime(US))
    assertEquals(Time.ZeroHours, qs1.usedTime(AU))
    assertEquals(Time.hours(GS.percentDoubleAt(site) - 10), qs1.remainingTime(GS))
    assertEquals(Time.hours(US.percentDoubleAt(site) - 20), qs1.remainingTime(US))
    assertEquals(Time.hours(AU.percentDoubleAt(site)), qs1.remainingTime(AU))

    assertEquals(Time.hours(50), qs1.remainingTime(Guaranteed))
    assertEquals(Time.hours(US.percentDoubleAt(site) * 0.8 - 20.0), qs1.remainingTime(Guaranteed, US))
    assertEquals(Time.hours(GS.percentDoubleAt(site) * 0.8 - 10.0), qs1.remainingTime(Guaranteed, GS))
    assertEquals(Time.hours(AU.percentDoubleAt(site) * 0.8), qs1.remainingTime(Guaranteed, AU))

    assertEquals(Time.hours(20), qs1.remainingTime(PoorWeather))
    assertEquals(Time.hours(US.percentDoubleAt(site) * 0.2), qs1.remainingTime(PoorWeather, US))
    assertEquals(Time.hours(GS.percentDoubleAt(site) * 0.2), qs1.remainingTime(PoorWeather, GS))
    assertEquals(Time.hours(AU.percentDoubleAt(site) * 0.2), qs1.remainingTime(PoorWeather, AU))
  }

  @Test def testAddAllProposals() {
    val prop1 = mkProp(10, "gs1")
    val prop2 = mkProp(20, "gs2")
    val prop3 = mkProp(30, "gs3")

    val pos1 = ProposalPosition(qs.queueTime)
    val pos2 = ProposalPosition(1, Time.hours(10), QBand1, 1, Time.hours(10))
    val pos3 = ProposalPosition(2, Time.hours(30), QBand2, 0, Time.hours(0))

    val lst = List(prop1, prop2, prop3)

    val qs1 = qs ++ lst

    assertEquals(Time.hours(60), qs1.usedTime)
    assertEquals(Time.hours(40), qs1.remainingTime)
    assertEquals(Time.hours(20), qs1.remainingTime(Guaranteed))
    assertEquals(List(prop1, prop2, prop3), qs1.toList)
    assertEquals(mkBandedQueue(List(prop1, prop2), List(prop3), Nil, Nil), qs1.bandedQueue)
    assertEquals(QBand3, qs1.band)

    assertEquals(pos1, qs1.positionOf(prop1).get)
    assertEquals(pos2, qs1.positionOf(prop2).get)
    assertEquals(pos3, qs1.positionOf(prop3).get)

    assertEquals(List((prop1, pos1), (prop2, pos2), (prop3, pos3)), qs1.zipWithPosition)

    verifyTimes(30, 30, 0, 0, qs1.usedTime)
    verifyTimes(0, 0, 20, 20, qs1.remainingTime _)
  }

  // Test normal situation where a proposal straddles the band1/band2 boundary
  @Test def testBand1Band2Straddle() {
    val prop_b1_0 = mkProp(20, "gs1") // Band 1, 0
    val prop_b1_1 = mkProp(20, "gs2") // Band 1, 1
    val prop_b2_0 = mkProp(10, "gs3") // Band 2, 0

    val pos1 = ProposalPosition(qs.queueTime)
    val pos2 = ProposalPosition(1, Time.hours(20), QBand1, 1, Time.hours(20))
    val pos3 = ProposalPosition(2, Time.hours(40), QBand2, 0, Time.hours(0))

    val qs1 = qs :+ prop_b1_0 // first band 1
    assertEquals(QBand1, qs1.band)

    val qs2 = qs1 :+ prop_b1_1 // second band 1 spills over
    assertEquals(QBand2, qs2.band)

    val qs3 = qs2 :+ prop_b2_0 // first band 2
    assertEquals(QBand2, qs3.band)

    assertEquals(mkBandedQueue(List(prop_b1_0, prop_b1_1), List(prop_b2_0), Nil, Nil), qs3.bandedQueue)
    assertEquals(List(prop_b1_0, prop_b1_1, prop_b2_0), qs3.toList)

    assertEquals(pos1, qs3.positionOf(prop_b1_0).get)
    assertEquals(pos2, qs3.positionOf(prop_b1_1).get)
    assertEquals(pos3, qs3.positionOf(prop_b2_0).get)

    verifyTimes(40, 10, 0, 0, qs3.usedTime)
    verifyTimes(-10, 20, 20, 20, qs3.remainingTime _)
  }

  //  Test that we move to Band 2 when we exactly fill the band 1
  @Test def testBand1ExactFill() {
    val prop_b1 = mkProp(30, "gs1")

    val qs1 = qs :+ prop_b1
    assertEquals(QBand2, qs1.band) // next proposal goes in band2

    assertEquals(mkBandedQueue(List(prop_b1), Nil, Nil, Nil), qs1.bandedQueue)
    assertEquals(List(prop_b1), qs1.toList)

    verifyTimes(30, 0, 0, 0, qs1.usedTime)
    verifyTimes(0, 30, 20, 20, qs1.remainingTime _)
  }

  // Test that band3 time is used once we move to band3
  @Test def testBand3Time() {
    assertEquals(Time.Zero, qs.usedTime(GS))

    val prop_b1 = mkProp(60, "gs1")

    val qs1 = qs :+ prop_b1
    assertEquals(Time.hours(60), qs1.usedTime(GS))
    assertEquals(Time.hours(40), qs1.remainingTime(GS))
    assertEquals(Time.hours(20), qs1.remainingTime(Guaranteed, GS))
    assertEquals(QBand3, qs1.band)

    // always use ntac time
    //    val prop_b3 = mkB3Prop(20, 10)
    val prop_b3 = mkB3Prop(10, "gs2")

    val qs3 = qs1 :+ prop_b3
    assertEquals(Time.hours(70), qs3.usedTime(GS))
    assertEquals(Time.hours(30), qs3.remainingTime(GS))
    assertEquals(Time.hours(10), qs3.remainingTime(Guaranteed, GS))
    assertEquals(QBand3, qs3.band)

    assertEquals(Time.hours(30), qs3.remainingTime)
    assertEquals(Time.hours(10), qs3.remainingTime(Guaranteed))
    assertEquals(mkBandedQueue(List(prop_b1), Nil, List(prop_b3), Nil), qs3.bandedQueue)
    assertEquals(List(prop_b1, prop_b3), qs3.toList)

    val pos1 = ProposalPosition(qs.queueTime)
    val pos2 = ProposalPosition(1, Time.hours(60), QBand3, 0, Time.Zero)
    assertEquals(pos1, qs3.positionOf(prop_b1).get)
    assertEquals(pos2, qs3.positionOf(prop_b3).get)

    verifyTimes(60, 0, 10, 0, qs3.usedTime)
    verifyTimes(-30, 30, 10, 20, qs3.remainingTime _)
  }

  // Try adding a non-band3 proposal at band 3 time.
  @Test def testNotBand3Failure() {
    val prop_b1 = mkProp(60, "gs1")
    val qs1 = qs :+ prop_b1

    val prop_b3 = mkProp(20, "gs2") // does not allow itself to be scheduled in band3
    try {
      qs1 :+ prop_b3
      fail()
    } catch {
      case ex: IllegalArgumentException => // expected
    }
  }

  // Try adding a proposal when all schedulable time has been allocated.
  @Test def testOverAllocate() {
    val prop_b1 = mkProp(60, "gs1")
    val qs1 = qs :+ prop_b1

    val prop_b3 = mkB3Prop(20, "gs2") // takes us to the limit (60 + 40)
    val qs2 = qs1 :+ prop_b3

    assertEquals(Time.hours(20), qs2.remainingTime)
    assertEquals(Time.Zero, qs2.remainingTime(Guaranteed)) // no more queue time

    assertEquals(QBand4, qs2.band)
  }

  @Test def testOverAllocateFailure() {
    val prop_b1 = mkProp(60, "gs1")
    val qs1 = qs :+ prop_b1

    assertEquals(QBand3, qs1.band)
    val prop_b3 = mkB3Prop(41, "gs2") // would add too much guaranteed time
    try {
      qs1 :+ prop_b3
      fail()
    } catch {
      case ex: IllegalArgumentException => // expected
    }
  }

  // Try adding a proposal that takes us over the schedulable time but under the
  // total full queue limit.  In other words, we should be able to allow the
  // last proposal to go over the band 3 limit (as we do for the last band 1
  // and band 2 proposals).
  @Test def testBand3SoftLimit() {
    val prop_b1 = mkProp(60, "gs1")
    val qs1 = qs :+ prop_b1

    val prop_b3 = mkB3Prop(30, "gs2") // takes us past the limit (80)
    val qs2 = qs1 :+ prop_b3

    assertEquals(Time.hours(10), qs2.remainingTime)
    assertEquals(Time.hours(-10), qs2.remainingTime(Guaranteed)) // over allocated
  }

  // Try adding a proposal in band 4 that goes over the total time.  Should
  // be allowed.
  @Test def testBand4SoftLimit() {
    val prop_b1 = mkProp(81, "gs1")
    val qs1 = qs :+ prop_b1
    assertEquals(QBand4, qs1.band)

    val prop_b4 = mkProp(20, "gs2") // takes us past the limit (100)
    val qs2 = qs1 :+ prop_b4

    assertEquals(Time.hours(-1), qs2.remainingTime)
    assertEquals(Time.hours(-1), qs2.remainingTime(Guaranteed)) // over allocated
  }

  @Test def testFilter() {
    val pAR = mkProp(AR, 1, "ar1")
    val pAU = mkProp(AU, 10, "au1")
    val pBR = mkProp(BR, 2, "br1")
    val pCA = mkProp(CA, 10, "ca1")
    val pGS = mkProp(GS, 3, "gs1")

    val qs1 = qs ++ List(pAR, pAU, pBR, pCA, pGS)

    // Filter on index and proposal time.  Keep only proposals whose index+1
    // is the same as the proposal time.
    val qs2 = qs1.positionFilter((prop, pos) => (pos.index + 1) == prop.time.toHours.value.toInt)

    assertEquals(List(pAR, pBR, pGS), qs2.toList)
    assertEquals(Time.hours(6), qs2.usedTime)
    assertEquals(Time.hours(3), qs2.usedTime(GS))
    assertEquals(Time.ZeroHours, qs2.usedTime(AU))

    // Don't filter out anything
    val qs3 = qs1.positionFilter((prop, pos) => true)
    assertEquals(List(pAR, pAU, pBR, pCA, pGS), qs3.toList)
    assertEquals(Time.hours(26), qs3.usedTime)

    // Filter out everything
    val qs4 = qs1.positionFilter((prop, pos) => false)
    assertEquals(Nil, qs4.toList)
    assertEquals(Time.ZeroHours, qs4.usedTime)
  }
}
