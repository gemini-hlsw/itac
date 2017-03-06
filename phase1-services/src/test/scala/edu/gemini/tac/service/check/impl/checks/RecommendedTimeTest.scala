package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._

import edu.gemini.tac.service.check.impl.checks.{RecommendedTimeCheck => Ck}

import collection.JavaConverters._
import edu.gemini.tac.persistence.phase1._
import edu.gemini.tac.service.check.ProposalIssue
import edu.gemini.tac.persistence.{ObservingMode, Proposal}
import edu.gemini.tac.persistence.fixtures.builders.{QueueProposalBuilder, ClassicalProposalBuilder, ProposalBuilder}
import edu.gemini.tac.persistence.joints.JointProposal

class RecommendedTimeTest {

  @Test def testZeroRecommendedTime() {
    val p = proposal(0.0, 1.0, 1.0)
    //    verify(Ck.zeroRecommendedTime(p), p)
    // ITAC-264: zero recommended time is okay.
    assertEquals(0, Ck(p).size)
  }

  @Test def testNegativeRecommendedTime() {
    val p = proposal(-0.1, 1.0, 1.0)
    verify(Ck.negativeRecommendedTime(p), p)
  }

  @Test def testLowRecommendedQueueTimeWithinMargin() {
    val p = proposal(1.91, 2.0, 2.0)
    assertEquals(0, Ck(p).size)
  }

  @Test def testLowRecommendedQueueTimeJustOutsideOfMargin() {
    val p = proposal(1.89, 2.0, 2.0)
    val rec = new TimeAmount(1.89, TimeUnit.HR)
    val req = new TimeAmount(2.0, TimeUnit.HR)
    verify(Ck.lowRecommendedTime(p, rec, req), p)
  }

  @Test
  def lowRecommendedWarningOnJointOnlyNotComponent() {
    val componentOne = new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      setRequest(new TimeAmount(2, TimeUnit.HR), new TimeAmount(2, TimeUnit.HR)).
      setAccept(new TimeAmount(1, TimeUnit.HR), new TimeAmount(1, TimeUnit.HR), 1).
      create()

    val componentTwo = new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerBR).
      setRequest(new TimeAmount(2, TimeUnit.HR), new TimeAmount(2, TimeUnit.HR)).
      setAccept(new TimeAmount(1, TimeUnit.HR), new TimeAmount(1, TimeUnit.HR), 1).
      create()

    val joint = new JointProposal()
    joint.add(componentOne)
    componentOne.setJointProposal(joint)
    joint.add(componentTwo)
    joint.setPrimaryProposal(componentOne)
    componentTwo.setJointProposal(joint)

    assert(componentOne.isJointComponent)
    assert(componentTwo.isJointComponent)

    verify(Ck.lowRecommendedTime(joint, new TimeAmount(2.0, TimeUnit.HR), new TimeAmount(4.0, TimeUnit.HR)), joint)
    val warnings = Ck(componentOne)
    assertEquals(0, warnings.size)
  }

  @Test def testLowRecommendedClassicalTimeWithinMargin() {
    val rec = (new TimeAmount(1.5, TimeUnit.NIGHT)).convertTo(TimeUnit.HR)
    val req = (new TimeAmount(2.0, TimeUnit.NIGHT)).convertTo(TimeUnit.HR)
    val p = classicalProposal(rec.getDoubleValue, req.getDoubleValue)
    assertEquals(0, Ck(p).size)
  }

  @Test def testLowRecommendedClassicalTimeJustOutsideOfMargin() {
    val rec = (new TimeAmount(1.49, TimeUnit.NIGHT)).convertTo(TimeUnit.HR)
    val req = (new TimeAmount(2.00, TimeUnit.NIGHT)).convertTo(TimeUnit.HR)
    val p = classicalProposal(rec.getDoubleValue, req.getDoubleValue)
    verify(Ck.lowRecommendedTime(p, rec, req), p)
  }

  @Test def testLowRecommendedTime() {
    val p = proposal(1.0, 2.0, 2.0)
    val rec = new TimeAmount(1.0, TimeUnit.HR)
    val req = new TimeAmount(2.0, TimeUnit.HR)
    verify(Ck.lowRecommendedTime(p, rec, req), p)
  }

  @Test def testGoodRecommendedTime() {
    val p = proposal(1.0, 1.0, 1.0)
    assertEquals(0, Ck(p).size)
  }

  @Test def testOverRecommendedTime() {
    val p = proposal(2.0, 1.0, 1.0)
    assertEquals(0, Ck(p).size)
  }

  @Test def testOKHighHours() {
    val p = proposal(99.0, 99.0, 99.0)
    assertEquals(0, Ck(p).size)
  }

  @Test def testFlagsTooManyHours() {
    val p = proposal(100.0, 100.0, 100.0)
    assertEquals(1, Ck(p).size)
  }

  def proposal(rec: Double, req: Double, minReq: Double) =
    new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      setRequest(new TimeAmount(minReq, TimeUnit.HR), new TimeAmount(req, TimeUnit.HR)).
      setAccept(new TimeAmount(rec, TimeUnit.HR), new TimeAmount(rec, TimeUnit.HR), 1).
      create()

  def classicalProposal(rec: Double, req: Double) =
    new ClassicalProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      setRequest(new TimeAmount(req, TimeUnit.HR), new TimeAmount(req, TimeUnit.HR)).
      setAccept(new TimeAmount(rec, TimeUnit.HR), new TimeAmount(rec, TimeUnit.HR), 1).
      create()

  private def verify(issue: ProposalIssue, p: Proposal) {
    assertEquals(Set(issue), Ck(p))
  }


}