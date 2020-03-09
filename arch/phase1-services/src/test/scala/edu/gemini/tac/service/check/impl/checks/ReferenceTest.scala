package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._

import edu.gemini.tac.persistence.phase1._

import edu.gemini.tac.service.check.impl.checks.{ReferenceCheck => Ck}
import edu.gemini.tac.persistence.{Partner, Proposal}
import edu.gemini.tac.persistence.fixtures.builders.{ProposalBuilder, QueueProposalBuilder}
import ProposalBuilder.{dummyPartnerUS => US, dummyPartnerCL => CL}


class ReferenceTest {
  def proposal(partner: Partner, ref: String): Proposal = 
    new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(partner).
      setSubmissionKey(ref).
      create()

  @Test def testStateUpdated() {
    val p = proposal(US, "ABC1")
    val (issues, state) = Ck(p, Ck.initialState)

    // State now includes the partner reference.
    assertEquals(Set(PartnerReference(US, "ABC1", "North")), state)

    // No issues.
    assertEquals(Set.empty, issues)
  }

  @Test def testDuplicateRejected() {
    val p = proposal(US, "ABC1")
    val r = PartnerReference(US, "ABC1", "North")
    val s = Set(r)
    val (issues, state) = Ck(p, s)

    // State not changed
    assertEquals(state, s)

    // Issue generated for duplicate reference.
    assertEquals(Ck.duplicate(p, r), issues)
  }

  @Test def testNoReferenceString() {
    val p = proposal(US, null)
    val (issues, state) = Ck(p, Ck.initialState)

    // State isn't updated because we found no submission partner
    assertEquals(state, Ck.initialState)

    // There should be a single "missing" message.
    assertEquals(Ck.missing(p), issues)
  }
}