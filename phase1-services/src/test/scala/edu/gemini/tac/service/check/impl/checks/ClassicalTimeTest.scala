package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._

import edu.gemini.tac.service.check.impl.checks.{ClassicalTimeRequestCheck => Ck}


import edu.gemini.tac.service.check.impl.core.ProposalIssue.noIssues
import edu.gemini.tac.persistence.phase1.{TimeUnit, TimeAmount}
import edu.gemini.tac.persistence.fixtures.builders.{ClassicalProposalBuilder, ProposalBuilder, QueueProposalBuilder}
import edu.gemini.tac.persistence.phase1.TimeUnit.{HR => HOURS, NIGHT => NIGHTS}

class ClassicalTimeTest {

  @Test def testQueueHoursPasses() {
    val p = queueProposal(42, HOURS)
    assertEquals(noIssues, Ck(p))
  }

  @Test def testClassicalNightsPasses() {
    val p = classicalProposal(42, NIGHTS)
    assertEquals(noIssues, Ck(p))
  }

  @Test def testClassicalMultipleOfTenHoursPasses() {
    val p = classicalProposal(40, HOURS)
    assertEquals(noIssues, Ck(p))
  }

  @Test def testClassicalNonMultipleOfTenHoursFails() {
    val p = classicalProposal(42, HOURS)
    assertEquals(Ck.notMultipleOfTen(p, 42), Ck(p))
  }

  @Test def testClassicalFractionalHoursFails() {
    val p = classicalProposal(10.001, HOURS)
    assertEquals(Ck.notMultipleOfTen(p, 10.001), Ck(p))
  }

  def queueProposal(req: Double, unit: TimeUnit) =
    new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      setRequest(new TimeAmount(req, unit), new TimeAmount(req, unit)).
      setAccept(new TimeAmount(req, unit), new TimeAmount(req, unit), 1).
      create()

  def classicalProposal(req: Double, unit: TimeUnit) =
    new ClassicalProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      setRequest(new TimeAmount(req, unit), new TimeAmount(req, unit)).
      setAccept(new TimeAmount(req, unit), new TimeAmount(req, unit), 1).
      create()

}