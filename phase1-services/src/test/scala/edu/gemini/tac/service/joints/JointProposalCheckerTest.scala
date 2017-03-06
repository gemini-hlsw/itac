package edu.gemini.tac.service.joints

import org.junit.{Assert, Test}
import edu.gemini.tac.persistence.{Proposal, Partner}
import java.util.Date
import edu.gemini.tac.persistence.fixtures.builders.{JointProposalBuilder, QueueProposalBuilder, ProposalBuilder}
import edu.gemini.tac.persistence.joints.JointProposal
import edu.gemini.tac.persistence.phase1.{Observation, TimeUnit, TimeAmount}


class JointProposalCheckerTest {
  val committee = ProposalBuilder.dummyCommittee

  def mkProposal(partner: Partner, ref: String, exposureTimes : List[Int]): Proposal =
    {
      val proposalBuilder = new QueueProposalBuilder().
        setCommittee(committee).
        setPartner(partner).
        setAccept(new TimeAmount(10, TimeUnit.HR), new TimeAmount(10, TimeUnit.HR), 1).
        setReceipt(ref, new Date())
      exposureTimes.map(x => proposalBuilder.addObservation("Vega", "00:00:00.000", "00:00:00.000", x, TimeUnit.HR))
      proposalBuilder.create()
    }

  def buildJointProposal(obs1 : List[Observation], obs2 : List[Observation]): JointProposal = {
    val jointAr1 = mkProposal(ProposalBuilder.dummyPartnerAR, "ar1", List(1,1,2))
    val jointBr1 = mkProposal(ProposalBuilder.dummyPartnerBR, "br1", List(2,1,1))
    new JointProposalBuilder().
      addProposal(jointAr1).
      addProposal(jointBr1).
      create()
  }

  @Test
  def canCompareJointProposal() {
    val checker = new JointProposalChecker()
    val obs = List()
    val jp = buildJointProposal(obs, obs)
    val ps = new java.util.ArrayList[Proposal]()
    ps.add(jp)
    val results = checker.exec(ps)
    Assert.assertNotNull(results)
    Assert.assertEquals(0, results.size())
  }
}