package edu.gemini.tac.qengine.impl

import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.QueueBand.Category._
import edu.gemini.tac.qengine.log.{RejectNotBand3, RejectNoTime, RejectNoObs, ProposalLog}
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.Site

class ProposalPrepTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._

  val target: Target = Target(0.0, 0.0)
  val conds: ObservingConditions = ObservingConditions.AnyConditions
  val noneBand3: List[Observation] = List.empty
  val someBand3: List[Observation] = List(Observation(null, target, conds, Time.hours(1)))
  val noneObs: List[Observation] = Nil
  val someObs: List[Observation] = List(Observation(null, target, conds, Time.hours(1)))

  private def nonJoint(partner: Partner, id: String, time: Time = Time.hours(1), b3: List[Observation] = Nil, obsList: List[Observation] = Nil): CoreProposal =
    CoreProposal(Ntac(partner, id, 0, time), site = Site.GS, band3Observations = b3, obsList = obsList)

  @Test def testEmpty() {
    val pp = ProposalPrep(Nil)
    assertEquals(Nil, pp.propList)
    assertEquals(Nil, pp.log.toList)
  }

  @Test def testNoObs() {
    val br = nonJoint(BR, "br1", obsList = noneObs)
    val pp = ProposalPrep(List(br))
    assertEquals(Nil, pp.propList)

    val br1key = ProposalLog.Key(Proposal.Id(BR, "br1"), B1_2)
    pp.log.toList match {
      case ProposalLog.Entry(key, msg: RejectNoObs) :: Nil => {
        assertEquals(br1key, key)
        assertEquals(br, msg.prop)
      }
      case _ => fail()
    }
  }

  @Test def testNoTime() {
    val br = nonJoint(BR, "br1", time = Time.Zero, obsList = someObs)
    val pp = ProposalPrep(List(br))
    assertEquals(Nil, pp.propList)

    val br1key = ProposalLog.Key(br.id, B1_2)
    pp.log.toList match {
      case ProposalLog.Entry(key, msg: RejectNoTime) :: Nil => {
        assertEquals(br1key, key)
        assertEquals(br, msg.prop)
      }
      case _ => fail()
    }
  }

  @Test def testB1_2() {
    val br = nonJoint(BR, "br1", time = Time.Zero, obsList = someObs)
    val ca = nonJoint(CA, "ca1", time = Time.hours(1), obsList = noneObs)
    val gs = nonJoint(GS, "gs1", time = Time.hours(1), obsList = someObs)

    val pp = ProposalPrep(List(br, ca, gs))
    assertEquals(List(gs), pp.propList)

    pp.log.get(br.id, B1_2) match {
      case Some(msg: RejectNoTime) => assertEquals(br, msg.prop)
      case _ => fail()
    }
    pp.log.get(ca.id, B1_2) match {
      case Some(msg: RejectNoObs) => assertEquals(ca, msg.prop)
      case _ => fail()
    }
  }

  @Test def testB3() {
    val br = nonJoint(BR, "br1", time = Time.Zero, obsList = someObs, b3 = noneBand3)
    val ca = nonJoint(CA, "ca1", time = Time.hours(1), obsList = noneObs, b3 = noneBand3)
    val gs = nonJoint(GS, "gs1", time = Time.hours(1), obsList = someObs, b3 = noneBand3)
    val uh = nonJoint(UH, "uh1", time = Time.hours(1), obsList = someObs, b3 = someBand3)

    val pp = ProposalPrep(List(br, ca, gs, uh)).band3(None)
    assertEquals(List(uh), pp.propList)

    pp.log.get(gs.id, B3) match {
      case Some(msg: RejectNotBand3) => assertEquals(gs, msg.prop)
      case _ => fail()
    }
  }
}
