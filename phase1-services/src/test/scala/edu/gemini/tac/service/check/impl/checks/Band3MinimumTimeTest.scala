package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._

import edu.gemini.tac.service.check.impl.core.ProposalIssue.noIssues
import edu.gemini.tac.service.check.impl.checks.{Band3MinimumTimeCheck => Ck}
import collection.JavaConverters._
import edu.gemini.tac.persistence.phase1._
import edu.gemini.model.p1.mutable.Band
import edu.gemini.tac.persistence.fixtures.builders.{JointProposalBuilder, ProposalBuilder, QueueProposalBuilder}
import edu.gemini.tac.persistence.joints.JointProposal
import ProposalBuilder.{dummyCommittee=>committee, dummyPartnerCL=>CL, dummyPartnerUS=>US}
import edu.gemini.tac.persistence.{Partner, Proposal}

class Band3MinimumTimeTest {

  private def proposal(totalBand3Time: Option[TimeAmount], minRecommendTime: TimeAmount, partner: Partner = US): Proposal = {
    val builder = 
      new QueueProposalBuilder().
        setCommittee(committee).
        setPartner(partner)
      if (totalBand3Time.isDefined) {
        builder.setBand(Band.BAND_3)
        builder.addObservation("0:0:0.0", "0:0:0.0", totalBand3Time.get)
        builder.setBand3Request(totalBand3Time.get, totalBand3Time.get)
      }
      builder.setAccept(minRecommendTime, minRecommendTime, 1)
      builder.create()
  }

  private def jointProposal(totalBand3Time: TimeAmount, minRecTimeUS: TimeAmount, minRecTimeCL: TimeAmount): JointProposal = {
    val builder = new JointProposalBuilder();
    builder.addProposal(proposal(Some(totalBand3Time), minRecTimeUS, US))
    builder.addProposal(proposal(Some(totalBand3Time), minRecTimeCL, CL))
    builder.create();
  }

  @Test def testNoBand3MeansNoIssues() {
    assertEquals(noIssues, Ck(proposal(None, new TimeAmount(2, TimeUnit.HR))))
  }

  @Test def testNoBand3NotAllowedMeansNoIssues() {
    assertEquals(noIssues, Ck(proposal(None, new TimeAmount(-5, TimeUnit.HR))))
  }

  @Test def testNegativeMinIsAnIssue() {
    val t = new TimeAmount(-5, TimeUnit.HR)
    val p = proposal(Some(t), t)
    assertEquals(Set(Ck.nonPositiveMinimumIssue(p, t)), Ck(p))
  }

  @Test def testZeroMinIsAnIssue() {
    val t = new TimeAmount(0, TimeUnit.HR)
    val p = proposal(Some(t), t)
    assertEquals(Set(Ck.nonPositiveMinimumIssue(p, t)), Ck(p))
  }

  @Test def testPositiveMinPasses() {
    val t = new TimeAmount(0.1, TimeUnit.HR)
    val p = proposal(Some(t), t)
    assertEquals(noIssues, Ck(p))
  }

  @Test def testB3LessThanSumOfRecommendedTimesWithSingleTacExtension() {
    val b3Min = new TimeAmount(0.1, TimeUnit.HR)
    val ext1  = new TimeAmount(1.0, TimeUnit.HR)

    val p = proposal(Some(b3Min), ext1)

    assertEquals(noIssues, Ck(p))
  }

  @Test def testB3IgnoredWhenNoRecommendedTime() {
    val b3Min = new TimeAmount(100.0, TimeUnit.HR)
    val ext1  = new TimeAmount(0.0, TimeUnit.HR)

    val p = proposal(Some(b3Min), ext1)

    assertEquals(noIssues, Ck(p))
  }

  @Test def testB3WithinMarginOfError() {
    val b3Min = new TimeAmount(1.00, TimeUnit.HR)
    val ext1  = new TimeAmount(0.91, TimeUnit.HR)

    val p = proposal(Some(b3Min), ext1)

    assertEquals(noIssues, Ck(p))
  }

  @Test def testB3JustOutsideMarginOfError() {
    val b3Min = new TimeAmount(1.00, TimeUnit.HR)
    val ext1  = new TimeAmount(0.89, TimeUnit.HR)

    val p = proposal(Some(b3Min), ext1)

    val issue = Ck.minimumMoreThanRecommendedIssue(p, b3Min, ext1)
    assertEquals(Set(issue), Ck(p))
  }

  @Test def testB3LessThanSumOfRecommendedTimesWithMultipleTacExtensions() {
    val b3Min = new TimeAmount(1.00, TimeUnit.HR)
    val ext1  = new TimeAmount(0.75, TimeUnit.HR)
    val ext2  = new TimeAmount(0.75, TimeUnit.HR)

    val p = jointProposal(b3Min, ext1, ext2)

    assertEquals(noIssues, Ck(p))
  }

  @Test def testB3MoreThanSumOfRecommendedTimesGeneratesError() {
    val b3Min = new TimeAmount(2.00, TimeUnit.HR)
    val ext1  = new TimeAmount(0.75, TimeUnit.HR)
    val ext2  = new TimeAmount(0.75, TimeUnit.HR)

    val p = jointProposal(b3Min, ext1, ext2)

    // Compare the string messages.  The time value is ext1 + ext2, but because
    // Time equals
    import RichTime._
    val issue = Ck.minimumMoreThanRecommendedIssue(p, b3Min, ext1 + ext2)
    assertEquals(Set(issue), Ck(p))
  }

  @Test def testB3EqualToSumOfRecommendedTimes() {
    val ext1  = new TimeAmount(0.75, TimeUnit.HR)
    val ext2  = new TimeAmount(0.75, TimeUnit.HR)

    import RichTime._
    val b3Min = ext1 + ext2

    val p = jointProposal(b3Min, ext1, ext2)

    assertEquals(noIssues, Ck(p))
  }

  @Test def testB3MoreThanSumOfRecommendedTimesForJointComponentNoError() {
    val b3Min = new TimeAmount(2.00, TimeUnit.HR)
    val ext1  = new TimeAmount(0.75, TimeUnit.HR)
    val ext2  = new TimeAmount(0.75, TimeUnit.HR)

    val p = jointProposal(b3Min, ext1, ext2)
    assertTrue(p.getPrimaryProposal.isJointComponent)

    // Compare the string messages.  The time value is ext1 + ext2, but because
    // Time equals
    assertEquals(noIssues, Ck(p.getPrimaryProposal))
  }
}