package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._
import edu.gemini.tac.service.check.impl.core.ProposalIssue

import edu.gemini.tac.service.check.impl.checks.{InstrumentCheck => Ck}
import edu.gemini.tac.persistence.Proposal
import edu.gemini.tac.persistence.phase1._

import blueprint.BlueprintBase
import edu.gemini.tac.persistence.fixtures.builders.{ProposalBuilder, QueueProposalBuilder}


class InstrumentsTest {

  @Test def testSingleSite() {
    val p = proposal(ProposalBuilder.GMOS_N_IMAGING)
    assertEquals(ProposalIssue.noIssues, Ck(p))
  }

  @Test def testSingleSiteMultipleInstruments() {
    val p = proposal(ProposalBuilder.GMOS_N_IMAGING, ProposalBuilder.GNIRS_IMAGING_H2_PS05)
    assertEquals(ProposalIssue.noIssues, Ck(p))
  }

  @Test def testMixedSites() {
    val p = proposal(ProposalBuilder.GMOS_N_IMAGING, ProposalBuilder.GMOS_S_LONGSLIT)
    assertEquals(Set(Ck.mixedSite(p)), Ck(p))
  }

// TODO: currently Keck and Subaru are not yet supported
//  @Test def testKeckAndSubaruTreatedAsGeminiNorth() {
//    val p = proposal("Gemini North", "Keck", "Subaru")
//    assertEquals(ProposalIssue.noIssues, Ck(p))
//  }

  def proposal(resourceTypes: BlueprintBase*): Proposal = {
    ProposalBuilder setDefaultPartner ProposalBuilder.dummyPartnerUS
    ProposalBuilder setDefaultCommittee ProposalBuilder.dummyCommittee
    val builder = new QueueProposalBuilder()
    resourceTypes.foreach(bp => {
      builder.
        setBlueprint(bp).
        addObservation("1:0:0", "1:0:0", new TimeAmount(1.0, TimeUnit.HR))
    })
    builder.create()
  }

}