package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._
import edu.gemini.tac.service.check.impl.core.ProposalIssue

import edu.gemini.tac.service.check.impl.checks.{TargetsCheck => Ck}
import edu.gemini.tac.persistence.phase1._

import edu.gemini.tac.persistence.fixtures.builders.{ProposalBuilder, QueueProposalBuilder}
import java.util.List

class TargetsTest {

  @Test def testNoTargets() {
    val p = emptyProposal()
    assertEquals(Ck.noTargets(p), Ck(p))
  }

  @Test def testSomeTargets() {
    val p = proposal()
    assertEquals(ProposalIssue.noIssues, Ck(p))
  }
  
  def emptyProposal() = {
    var p = new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      create();
    // destroy beautifully valid proposal by removing the default target
    p.getPhaseIProposal.setTargets(new java.util.ArrayList[Target]())
    p
  }

  def proposal() = 
    new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      create();
  

}