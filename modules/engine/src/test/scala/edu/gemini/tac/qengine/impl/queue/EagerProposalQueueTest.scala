package edu.gemini.tac.qengine.impl.queue

import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.QueueBand._
import CloudCover.CC50
import ImageQuality.IQ70
import SkyBackground.SB20
import WaterVapor.WV50
import edu.gemini.tac.qengine.api.config.QueueBandPercentages
import edu.gemini.tac.qengine.api.queue.ProposalPosition
import edu.gemini.tac.qengine.api.queue.time.{PartnerTime, QueueTime}
import edu.gemini.spModel.core.Site

/**
 * ProposalQueue tests focused on testing joint proposal merge features and
 * using the EagerMerge strategy.
 */
class EagerProposalQueueTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  // Need obsConds, but we don't use it for this test
  private val obsConds = ObservingConditions(CC50, IQ70, SB20, WV50)

  private def mkProp(propTimeHours: Int, ref: String): CoreProposal = {
    val ntac = Ntac(GS, ref, 0, Time.hours(propTimeHours))

    // Make a proposal with just no observations.  We won't be using them anyway.
    CoreProposal(ntac, site = Site.GS)
  }

  private def masterPart(propTimeHours: Int, ref: String, jointId: String): JointProposalPart = {
    val c = mkProp(propTimeHours, ref)
    JointProposalPart(jointId, c)
  }
  private def otherPart(propTimeHours: Int, ref: String, mp: JointProposalPart): JointProposalPart =
    JointProposalPart(mp.jointIdValue, mp.core.copy(ntac = Ntac(GS, ref, 0, Time.hours(propTimeHours))))

  private val qs = ProposalQueueBuilder(QueueTime(Site.GS, PartnerTime(partners, GS -> Time.hours(100)), QueueBandPercentages(30, 30, 40), Some(QueueTime.DefaultPartnerOverfillAllowance)), EagerMergeStrategy)
  private val qtime = qs.queueTime

  @Test def testPromotePart() {
    val prop1 = mkProp(    10, "gs1")
    val prop2 = masterPart(19, "gs2", "j1")
    val prop3 = mkProp(     2, "gs3")        // straddles band 1 / band 2 line
    val prop4 = otherPart(  2, "gs4", prop2) // promoted to band1, prop3 demoted to band2

    val qs1 = qs :+ prop1 :+ prop2 :+ prop3

    val pos1_1 = ProposalPosition(qtime)
    val pos1_2 = ProposalPosition(1, Time.hours(10), QBand1, 1, Time.hours(10))
    val pos1_3 = ProposalPosition(2, Time.hours(29), QBand1, 2, Time.hours(29))
    assertEquals(pos1_1, qs1.positionOf(prop1).get)
    assertEquals(pos1_2, qs1.positionOf(prop2).get)
    assertEquals(pos1_3, qs1.positionOf(prop3).get) // in band 1 now

    val j1_1 = prop2.toJoint
    val zip1 = List((prop1, pos1_1), (j1_1, pos1_2), (prop3, pos1_3))
    assertEquals(zip1, qs1.zipWithPosition)

    val qs2 = qs1 :+ prop4

    val pos2_1 = ProposalPosition(qtime)
    val pos2_2 = ProposalPosition(1, Time.hours(10), QBand1, 1, Time.hours(10))
    val pos2_3 = ProposalPosition(2, Time.hours(31), QBand2, 0, Time.hours( 0))

    assertEquals(pos2_1, qs2.positionOf(prop1).get)
    assertEquals(pos2_2, qs2.positionOf(prop2).get)
    assertEquals(pos2_3, qs2.positionOf(prop3).get) // moved down to band 2
    assertEquals(pos2_2, qs2.positionOf(prop4).get) // same as prop2

    val j1_2 = JointProposal.merge(List(prop2, prop4))
    val zip2 = List((prop1, pos2_1), (j1_2, pos2_2), (prop3, pos2_3))
    assertEquals(zip2, qs2.zipWithPosition)
  }

  @Test def testDemotePart() {
    val prop1 = mkProp(    29, "gs1")
    val prop2 = masterPart( 2, "gs2", "j1") // straddle band 1 / band 2
    val prop3 = mkProp(     2, "gs3")       // starts out in band 2, moved up
    val prop4 = otherPart( 26, "gs4", prop2) // demotes prop2 to band 2

    val qs1 = qs :+ prop1 :+ prop2 :+ prop3

    val pos1_1 = ProposalPosition(qtime)
    val pos1_2 = ProposalPosition(1, Time.hours(29), QBand1, 1, Time.hours(29))
    val pos1_3 = ProposalPosition(2, Time.hours(31), QBand2, 0, Time.hours( 0))
    assertEquals(pos1_1, qs1.positionOf(prop1).get)
    assertEquals(pos1_2, qs1.positionOf(prop2).get) // in band 1 for now
    assertEquals(pos1_3, qs1.positionOf(prop3).get) // in band 2 for now

    val j1_1 = prop2.toJoint
    val zip1 = List((prop1, pos1_1), (j1_1, pos1_2), (prop3, pos1_3))
    assertEquals(zip1, qs1.zipWithPosition)

    val qs2 = qs1 :+ prop4

    val pos2_1 = ProposalPosition(qtime)
    val pos2_2 = ProposalPosition(2, Time.hours(31), QBand2, 0, Time.hours( 0))
    val pos2_3 = ProposalPosition(1, Time.hours(29), QBand1, 1, Time.hours(29))

    assertEquals(pos2_1, qs2.positionOf(prop1).get)
    assertEquals(pos2_2, qs2.positionOf(prop2).get) // demoted to band 2
    assertEquals(pos2_3, qs2.positionOf(prop3).get) // promoted to band 1
    assertEquals(pos2_2, qs2.positionOf(prop4).get) // same as prop2

    val j1_2 = JointProposal.merge(List(prop2, prop4))
    val zip2 = List((prop1, pos2_1), (prop3, pos2_3), (j1_2, pos2_2))
    assertEquals(zip2, qs2.zipWithPosition)
  }
}