package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._
import java.math.BigDecimal

import edu.gemini.tac.service.check.impl.checks.{RankingCheck => Ck}
import edu.gemini.tac.persistence.fixtures.builders.{ProposalBuilder, QueueProposalBuilder}
import edu.gemini.tac.persistence.phase1.{TimeAmount, TimeUnit}
import edu.gemini.tac.persistence.{Partner}
import ProposalBuilder.{dummyPartnerUS => US, dummyPartnerCL => CL}

class RankingTest {
  @Test def testStateUpdated() {
    val p = proposal(US, 42)
    val (issues, state) = Ck(p, Ck.initialState)

    // State now includes the partner reference.
    assertEquals(Set(PartnerRanking(US, new BigDecimal(42), "North")), state)

    // No issues.
    assertEquals(Set.empty, issues)
  }

  @Test def testDuplicateRejected() {
    val p = proposal(US, 42)
    val r = PartnerRanking(US, new BigDecimal(42), "North")
    val s = Set(r)
    val (issues, state) = Ck(p, s)

    // State not changed
    assertEquals(state, s)

    // Issue generated for duplicate reference.
    assertEquals(Ck.duplicate(p, r), issues)
  }

  @Test def testDuplicateFromOtherCountryOK() {
    val p = proposal(US, 42)
    val r = PartnerRanking(CL, new BigDecimal(42), "North")
    val s = Set(r)
    val (issues, state) = Ck(p, s)

    // State has two entries (one for US, one for CL)
    assertEquals(2,state.size)

    // Issue generated for duplicate reference.
    assertEquals(Set.empty, issues)
  }
  @Test def testNoRankingString() {
    // create perfectly sound proposal and destroy it by setting ranking to NULL
    val p = proposal(US, 1)
    p.getPhaseIProposal.getPrimary.getAccept.setRanking(null)

    val (issues, state) = Ck(p, Ck.initialState)

    // State isn't updated because we found no ranking
    assertEquals(state, Ck.initialState)

    // There should be a single "missing" message.
    assertEquals(Ck.badRanking(p, null), issues)
  }

  def proposal(partner: Partner, rank: Int) =
    new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(partner).
      setAccept(new TimeAmount(1,TimeUnit.HR), new TimeAmount(1,TimeUnit.HR), rank).
      create()


}