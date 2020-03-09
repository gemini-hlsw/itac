package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._

import edu.gemini.tac.persistence.Proposal
import edu.gemini.tac.persistence.phase1._
import blueprint.BlueprintBase
import edu.gemini.tac.persistence.phase1.sitequality._
import edu.gemini.tac.persistence.fixtures.builders.{QueueProposalBuilder, ProposalBuilder}
import edu.gemini.model.p1.mutable.TooOption

import CloudCover.{   CC_50 => CC50, CC_70 => CC70, CC_80 => CC80, CC_100 => CCAny}
import ImageQuality.{ IQ_20 => IQ20, IQ_70 => IQ70, IQ_85 => IQ85, IQ_100 => IQAny}
import SkyBackground.{SB_20 => SB20, SB_50 => SB50, SB_80 => SB80, SB_100 => SBAny}
import WaterVapor.{   WV_20 => WV20, WV_50 => WV50, WV_80 => WV80, WV_100 => WVAny}

import edu.gemini.tac.service.check.impl.checks.{ConditionsCheck => Ck}
import edu.gemini.tac.service.check.impl.core.ProposalIssue.noIssues

import collection.JavaConverters._
import edu.gemini.tac.service.check.ProposalIssue

class ConditionsTest {

  def mkSq(cc: CloudCover, iq: ImageQuality, sb: SkyBackground, wv: WaterVapor): Condition = {
    val c = new Condition()
    c.setCloudCover(cc)
    c.setImageQuality(iq)
    c.setSkyBackground(sb)
    c.setWaterVapor(wv)
    c
  }

  def emptyProposal =  {
    val p = new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      create()
    // destroy beautifully valid proposal by removing observations
    p.getPhaseIProposal.setObservations(new java.util.HashSet[Observation]())
    p
  }

  def proposal(blueprint: BlueprintBase, condition: Condition) =
    new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      setBlueprint(blueprint).
      setCondition(condition).
      addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
      create()

  @Test def testEmptyProposal() {
    assertEquals(noIssues, Ck(emptyProposal))
  }

  private def issue(ct: Ck.ConditionsRule, sq: Condition, bp: BlueprintBase): (Proposal, Option[ProposalIssue]) = {
    val p      = proposal(bp, sq)
    val obs    = p.getPhaseIProposal.getObservations.iterator().next()
    (p, ct.issue(p, obs))
  }

  private def singleIssueTest(ct: Ck.ConditionsRule, pair: (Proposal, Option[ProposalIssue])) {
    pair match {
      case (_, None)     => fail()
      case (p, Some(is)) => assertEquals(Some(is), ct.issue(p, p.getObservations.iterator().next()))
    }
  }

  private def singleIssueTest(ct: Ck.ConditionsRule, sq: Condition, bp: BlueprintBase) {
    singleIssueTest(ct, issue(ct, sq, bp))
  }

  private def noIssuesTest(ct: Ck.ConditionsRule, sq: Condition, bp: BlueprintBase) {
    issue(ct, sq, bp) match {
      case (p, None)    => assertEquals(None, ct.issue(p, p.getObservations.iterator().next()))
      case (_, Some(_)) => fail()
    }
  }

  @Test def testBadIqCcIssue() {
    // NICI/LGS and bad IQ should trigger error regardless of CC.
    for {
      cc <- List(CC50, CC70, CC80, CCAny)
      iq <- List(IQ85, IQAny)
    } {
      val sq0 = mkSq(cc, iq, SBAny, WVAny)
      val (p0, is0) = issue(Ck.IqTest, sq0, ProposalBuilder.GNIRS_IMAGING_H2_PS05_LGS)
      singleIssueTest(Ck.IqTest, (p0, is0))
      assertTrue(is0.get.message.contains("image quality"))

      val sq1 = mkSq(cc, iq, SBAny, WVAny)
      val (p1, is1) = issue(Ck.IqTest, sq1, ProposalBuilder.NICI_STANDARD)
      singleIssueTest(Ck.IqTest, (p1, is1))
      assertTrue(is1.get.message.contains("image quality"))
    }

    // LGS and CC70 should trigger CC error.
    val sq2 = mkSq(CC70, IQ20, SBAny, WVAny)
    val (p2, is2) = issue(Ck.IqTest, sq2, ProposalBuilder.GNIRS_IMAGING_H2_PS05_LGS)
    singleIssueTest(Ck.IqTest, (p2, is2))
    assertTrue(is2.get.message.contains("cloud cover"))

    // NICI and CC80 should trigger error.
    val sq3 = mkSq(CC80, IQ20, SBAny, WVAny)
    val (p3, is3) = issue(Ck.IqTest, sq3, ProposalBuilder.NICI_STANDARD)
    singleIssueTest(Ck.IqTest,  (p3, is3))
    assertTrue(is3.get.message.contains("cloud cover"))
  }

  @Test def testBadIqCcNoIssue() {
    // Not NICI, not LGS, no problem
    for {
      cc <- List(CC50, CC70, CC80, CCAny)
      iq <- List(IQ20, IQ70, IQ85, IQAny)
    } {
      val sq = mkSq(cc, iq, SBAny, WVAny)
      noIssuesTest(Ck.IqTest, sq, ProposalBuilder.GNIRS_IMAGING_H2_PS05)
    }

    // IQ70/CC50 no problem for LGS or NICI
    val sq1 = mkSq(CC50, IQ70, SBAny, WVAny)
    noIssuesTest(Ck.IqTest, sq1, ProposalBuilder.NICI_STANDARD)
    noIssuesTest(Ck.IqTest, sq1, ProposalBuilder.GNIRS_IMAGING_H2_PS05_LGS)

    // IQ70/CC70 no problem for NICI.
    val sq2 = mkSq(CC70, IQ70, SBAny, WVAny)
    noIssuesTest(Ck.IqTest, sq2, ProposalBuilder.NICI_STANDARD)
  }

  @Test def testGmosSbIssue() {
    // GMOS with SBAny fails regardless of WV
    List(WV20, WV50, WV80, WVAny) foreach { wv =>
      val sq = mkSq(CC50, IQ85, SBAny, wv)
      singleIssueTest(Ck.GmosSbTest, sq, ProposalBuilder.GMOS_N_IMAGING)
    }

    // GMOS with good WV fails regardless of SB
    for {
      sb <- List(SB20, SB50, SB80, SBAny)
      wv <- List(WV20, WV50)
    } {
      val sq = mkSq(CC50, IQ85, sb, wv)
      singleIssueTest(Ck.GmosSbTest, sq, ProposalBuilder.GMOS_N_IMAGING)
    }
  }

  @Test def testGmosSbNoIssue() {
    // Not Gmos, no problem to have SBAny
    val sq0 = mkSq(CC50, IQ85, SBAny, WVAny)
    noIssuesTest(Ck.GmosSbTest, sq0, ProposalBuilder.GNIRS_IMAGING_H2_PS05)

    // Not Gmos, no problem to have good WV
    List(WV20, WV50) foreach { wv =>
      val sq = mkSq(CC50, IQ85, SBAny, wv)
      noIssuesTest(Ck.GmosSbTest, sq, ProposalBuilder.GNIRS_IMAGING_H2_PS05)
    }

    // GMOS with non-SBAny and bad WV passes.
    for {
      sb <- List(SB20, SB50, SB80)
      wv <- List(WV80, WVAny)
    } {
      val sq = mkSq(CC50, IQ85, sb, wv)
      noIssuesTest(Ck.GmosSbTest, sq, ProposalBuilder.GMOS_N_IMAGING)
    }
  }

  @Test def testNonGmosSbIssue() {
    // Not GMOS with good SB fails.
    List(SB20, SB50) foreach { sb =>
      val sq = mkSq(CC50, IQ85, sb, WVAny)
      singleIssueTest(Ck.NonGmosSbTest, sq, ProposalBuilder.GNIRS_IMAGING_H2_PS05)
    }
  }

  @Test def testNonGmosSbNoIssue() {
    // Not GMOS with bad SB passes.
    List(SB80, SBAny) foreach { sb =>
      val sq = mkSq(CC50, IQ85, sb, WVAny)
      noIssuesTest(Ck.NonGmosSbTest, sq, ProposalBuilder.GNIRS_IMAGING_H2_PS05)
    }
  }

  @Test def testGoodWvIssue() {
    // T-ReCS or Michelle with WVAny triggers a problem.
    List(ProposalBuilder.TRECS_IMAGING, ProposalBuilder.MICHELLE_IMAGING) foreach { inst =>
      val sq = mkSq(CC50, IQ20, SB80, WVAny)
      singleIssueTest(Ck.WvTest, sq, inst)
    }
  }

  @Test def testGoodWvNoIssue() {
    // T-ReCS or Michelle with good WV is no problem
    for {
      inst <- List(ProposalBuilder.TRECS_IMAGING, ProposalBuilder.MICHELLE_IMAGING)
      wv   <- List(WV20, WV50, WV80)
    } {
      val sq = mkSq(CC50, IQ20, SB80, wv)
      noIssuesTest(Ck.WvTest, sq, inst)
    }

    // Not T-ReCS or Michelle, bad WV not a problem.
    val sq = mkSq(CC50, IQ20, SB80, WVAny)
    noIssuesTest(Ck.WvTest, sq, ProposalBuilder.GNIRS_IMAGING_H2_PS05)
  }

  @Test def testSingleObservationTriggersMultipleIssues() {
    val sq  = mkSq(CC50, IQ85, SB20, WVAny)
    val p = proposal(ProposalBuilder.NICI_STANDARD, sq)

    val issues = Ck(p)
    assertEquals(2, issues.size)

    val messages = issues map { _.message }

    val expectedMessages = Set(
      Ck.IqTest.detailMessage(false, CC50, IQ85),
      Ck.NonGmosSbTest.detailMessage("NICI", SB20)
    )

    assertEquals(expectedMessages, messages)
  }
  
  // ---- check AoNotIqAnyTest
  
  @Test def altairWithIqAnyIsWarning() {
    singleIssueTest(Ck.AoNotIqAnyTest, ProposalBuilder.WORST_CONDITIONS, ProposalBuilder.GNIRS_IMAGING_H2_PS05_LGS)
  }

  @Test def altairWithIqBetterThanAnyIsFine() {
    noIssuesTest(Ck.AoNotIqAnyTest, ProposalBuilder.BEST_CONDITIONS, ProposalBuilder.GNIRS_IMAGING_H2_PS05_LGS)
  }

  // ---- check NotAllConditionsAnyTest

  @Test def noTooWithWorstConditionsIsWarning() {
    val p = tooOptionProposal(TooOption.NONE, CCAny, IQAny, SBAny, WVAny)
    assertEquals(1, Ck.NotAllConditionsAnyTest.issue(p, p.getObservations.iterator().next).size)
  }

  @Test def standardTooWithWorstConditionsIsWarning(){
    val p = tooOptionProposal(TooOption.STANDARD, CCAny, IQAny, SBAny, WVAny)
    assertEquals(1, Ck.NotAllConditionsAnyTest.issue(p, p.getObservations.iterator().next).size)
  }

  @Test def rapidTooWithWorstConditionsIsFine(){
    val p = tooOptionProposal(TooOption.RAPID, CCAny, IQAny, SBAny, WVAny)
    assertEquals(0, Ck.NotAllConditionsAnyTest.issue(p, p.getObservations.iterator().next).size)
  }

  @Test def noTooWithMixedConditionsIsFine(){
    val p = tooOptionProposal(TooOption.NONE, CC80, IQAny, SBAny, WVAny)
    assertEquals(0, Ck.NotAllConditionsAnyTest.issue(p, p.getObservations.iterator().next).size)
  }

  @Test def standardTooWithMixedConditionsIsFine(){
    val p = tooOptionProposal(TooOption.STANDARD, CC80, IQ70, SBAny, WVAny)
    assertEquals(0, Ck.NotAllConditionsAnyTest.issue(p, p.getObservations.iterator().next).size)
  }

  @Test def rapidTooWithMixedConditionsIsFine(){
    val p = tooOptionProposal(TooOption.RAPID, CCAny, IQ70, SBAny, WV50)
    assertEquals(0, Ck.NotAllConditionsAnyTest.issue(p, p.getObservations.iterator().next).size)
  }

  @Test def gsaoiWithCloudCoverIsBad(){
    val okConditions  = mkSq(CC50, IQ20, SB20, WVAny)
    val okProposal = proposal(ProposalBuilder.GSAOI, okConditions)

    val okIssues = Ck(okProposal)
    assertEquals(1, okIssues.size)

    val badConditions = mkSq(CC70, IQ20, SB20, WVAny)
    val badProposal = proposal(ProposalBuilder.GSAOI, badConditions)

    val issues = Ck(badProposal)

    assertEquals(3, issues.size)

    val messages = issues map { _.message }

    val expectedMessages = Set(
      Ck.NonGmosSbTest.detailMessage("GSAOI", SB20),
      Ck.IqTest.detailMessage(true, badConditions.getCloudCover, badConditions.getImageQuality),
      Ck.GsaoiCloudCoverConstraintTest.detailMessage(badConditions.getCloudCover)
    )

    assertEquals(expectedMessages, messages)
  }


  def tooOptionProposal(tooOption: TooOption, cc: CloudCover, iq: ImageQuality, sb: SkyBackground, wv: WaterVapor) =
    new QueueProposalBuilder(tooOption).
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      // the following blueprint and condition settings will be used
      // for default observation added automatically by builder
      setBlueprint(ProposalBuilder.GMOS_N_IMAGING).
      setCondition(ProposalBuilder.createCondition(cc, iq, sb, wv)).
      create()

  @Test def testMultipleObservationsAreHandledProperly() {
    val iqSq         = mkSq(CC50, IQ85, SBAny, WVAny)
    val gmosSbSq     = mkSq(CC50, IQ20, SBAny, WV20)
    val nonGmosSbSq  = mkSq(CCAny, IQAny, SB20, WVAny)
    val wvSq         = mkSq(CC50, IQ20, SBAny, WVAny)

    val p = new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      setBlueprint(ProposalBuilder.NICI_STANDARD).
      setCondition(iqSq).
      addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
      setBlueprint(ProposalBuilder.GMOS_N_IMAGING).
      setCondition(gmosSbSq).
      addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
      setBlueprint(ProposalBuilder.GNIRS_IMAGING_H2_PS05).
      setCondition(nonGmosSbSq).
      addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
      setBlueprint(ProposalBuilder.TRECS_IMAGING).
      setCondition(wvSq).
      addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
      create()

    val issues = Ck(p)
    assertEquals(4, issues.size)

    val messages = issues map { _.message }

    val expectedMessages = Set(
      Ck.IqTest.detailMessage(false, CC50, IQ85),
      Ck.GmosSbTest.detailMessage(SBAny, WV20),
      Ck.NonGmosSbTest.detailMessage("GNIRS", SB20),
      Ck.WvTest.detailMessage("T-ReCS")
    )

    assertEquals(expectedMessages, messages)
  }
}