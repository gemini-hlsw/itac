package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._
import edu.gemini.tac.persistence.Proposal
import edu.gemini.tac.persistence.phase1._
import edu.gemini.tac.service.check.impl.core.ProposalIssue.noIssues
import edu.gemini.tac.service.check.impl.checks.{Band3ConditionsCheck => Ck}
import edu.gemini.tac.persistence.fixtures.builders.{ProposalBuilder, QueueProposalBuilder}
import ProposalBuilder.{WORST_CONDITIONS => worstConditions, BEST_CONDITIONS => bestConditions}
import edu.gemini.model.p1.mutable.Band
import sitequality.{SkyBackground, CloudCover, WaterVapor, ImageQuality}

class Band3ConditionsTest {

  private def proposal(b12: List[(Target,Condition)], b3: List[(Target, Condition)]): Proposal = {
    val builder = new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS);
      b12.foreach(x => {
        builder.addObservation(Band.BAND_1_2, ProposalBuilder.GMOS_N_IMAGING, x._2, x._1, new TimeAmount(1, TimeUnit.HR));
      })
      b3.foreach(x => {
        builder.addObservation(Band.BAND_3, ProposalBuilder.GMOS_N_IMAGING, x._2, x._1, new TimeAmount(1, TimeUnit.HR));
      })
      builder.create()
  }

  private val iq20 = ProposalBuilder.createCondition(CloudCover.CC_50, ImageQuality.IQ_20, SkyBackground.SB_50, WaterVapor.WV_50)
  private val iq70 = ProposalBuilder.createCondition(CloudCover.CC_50, ImageQuality.IQ_70, SkyBackground.SB_50, WaterVapor.WV_50)
  private val iq85 = ProposalBuilder.createCondition(CloudCover.CC_50, ImageQuality.IQ_85, SkyBackground.SB_50, WaterVapor.WV_50)
  
  private val t1 = ProposalBuilder.createSiderealTarget("Target1", "1:0:0.0", "1:0:0.0")
  private val t2 = ProposalBuilder.createSiderealTarget("Target2", "2:0:0.0", "2:0:0.0")

  @Test def noIssuesWithoutObservations() {
    val b12 = List()
    val b3 = List()
    val p = proposal(b12, b3)
    assertEquals(noIssues, Ck(p))
  }

  @Test def noIssuesWithDifferentObservations() {
    val b12 = List((t1, worstConditions))
    val b3 = List((t2, bestConditions))
    val p = proposal(b12, b3)
    assertEquals(noIssues, Ck(p))
  }

  @Test def noIssuesWithoutBand3Observations() {
    val b12 = List((t1,iq20),(t2,iq70))
    val b3 = List()                           // no band3 observations at all
    val p = proposal(b12, b3)
    assertEquals(noIssues, Ck(p))
  }

  @Test def noIssuesWhenBand3SiteQualityIsWorst() {
    val b12 = List((t1,worstConditions))
    val b3 = List((t1,worstConditions))       // same  target in band12 and band3, same conditions
    val p = proposal(b12, b3)
    assertEquals(noIssues, Ck(p))
  }

  @Test def hasIssueWhenBand3SiteQualityIsBetter() {
    val b12 = List((t1,worstConditions))
    val b3 = List((t1,bestConditions))      // same  target in band12 and band3, b3 with all better conditions
    val p = proposal(b12, b3)
    val expIssues = Set(Ck.band3ConditionsIssue(p, (t1, worstConditions), (t1, bestConditions)))
    val issues = Ck(p)
    assertEquals(expIssues, issues)
  }

  @Test def hasIssueWhenBand3HasBetterIQ() {
    val b12 = List((t1,iq85))
    val b3 = List((t1,iq20))          // same  target in band12 and band3, b3 with better IQ conditions
    val p = proposal(b12, b3)
    val expIssues = Set(Ck.band3ConditionsIssue(p, (t1, iq85), (t1, iq20)))
    val issues = Ck(p)
    assertEquals(expIssues, issues)
  }

  @Test def testMakesAnIssueForAmbiguousSiteQualityCases() {
    val b3Conditions = ProposalBuilder.createCondition(CloudCover.CC_50, ImageQuality.IQ_85, SkyBackground.SB_50, WaterVapor.WV_20)
    val b12Conditions = ProposalBuilder.createCondition(CloudCover.CC_50, ImageQuality.IQ_20, SkyBackground.SB_50, WaterVapor.WV_50)
    val b12 = List((t1,b12Conditions))
    val b3 = List((t1,b3Conditions))
    val p = proposal(b12, b3)
    val expIssues = Set(Ck.band3ConditionsIssue(p, (t1, b12Conditions), (t1, b3Conditions)))
    val issues = Ck(p)
    assertEquals(expIssues, issues)
  }
}