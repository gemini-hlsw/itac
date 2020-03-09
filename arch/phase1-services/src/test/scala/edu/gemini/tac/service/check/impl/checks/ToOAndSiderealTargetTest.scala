package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._
import edu.gemini.tac.service.check.impl.core.ProposalIssue

import edu.gemini.tac.service.check.impl.checks.{MixedToOAndOtherTargetCheck => Ck}

import edu.gemini.tac.persistence.fixtures.builders.{ProposalBuilder, QueueProposalBuilder}
import edu.gemini.model.p1.mutable.CoordinatesEpoch
import edu.gemini.tac.persistence.Proposal
import java.util.{ArrayList, List}
import edu.gemini.tac.persistence.phase1._

class TooAndSiderealTargetTest {

  @Test def justTooAreFine() {
    val p = justTooProposal
    assertEquals(ProposalIssue.noIssues, Ck(p))
  }

  @Test def justSiderealAreFine(){
    val p = justSiderealProposal
    assertEquals(ProposalIssue.noIssues, Ck(p))
  }

  @Test def combinedTooAndSiderealAreWarned(){
    val p = combinedSiderealAndTooProposal
    assertEquals(Ck.mixedTargets(p), Ck(p))
  }

  @Test def combinedTooAndNonsiderealAreWarned() {
    val p = combinedNonsiderealAndTooProposal
    assertEquals(Ck.mixedTargets(p), Ck(p))
  }
  
  private def proposal: Proposal = {
    val p = new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      create();
    p
  }

  def justTooProposal = {
    val p: Proposal = proposal
      // destroy beautifully valid proposal by removing the default target
    p.getPhaseIProposal.setTargets(twoToOTargets)
    p
  }

  def justSiderealProposal = {
    val p = proposal
    p.getPhaseIProposal.setTargets(twoSiderealTargets)
    p
  }

  def combinedNonsiderealAndTooProposal = {
    val p = proposal
    val toos = twoToOTargets
    val sts = twoNonsiderealTargets
    val ts = new java.util.ArrayList[Target]()
    ts.addAll(toos)
    ts.addAll(sts)
    p.getPhaseIProposal.setTargets(ts)
    p
  }

  def combinedSiderealAndTooProposal = {
    val p = proposal
    val toos = twoToOTargets
    val sts = twoSiderealTargets
    val ts = new java.util.ArrayList[Target]()
    ts.addAll(toos)
    ts.addAll(sts)
    p.getPhaseIProposal.setTargets(ts)
    p
  }

  def twoToOTargets : java.util.ArrayList[Target] = {
    val ts = new java.util.ArrayList[Target]()
    ts.add(tooTarget("TooTarget1"))
    ts.add(tooTarget("TooTarget2"))
    ts
  }

  def tooTarget(nm : String) : Target = {
    val t = new TooTarget()
    t.setEpoch(CoordinatesEpoch.J_2000)
    t.setName(nm)
    t
  }

  def siderealTarget(nm : String) : Target = {
    val t = new SiderealTarget()
    t.setName(nm)
    t
  }

  def nonsiderealTarget(nm : String) : Target = {
    val t = new NonsiderealTarget()
    t.setName(nm)
    t
  }

  def twoSiderealTargets : java.util.ArrayList[Target] = {
    val ts = new java.util.ArrayList[Target]()
    ts.add(siderealTarget("Sidereal1"))
    ts.add(siderealTarget("Sidereal2"))
    ts
  }

  def twoNonsiderealTargets : java.util.ArrayList[Target] = {
    val ts = new java.util.ArrayList[Target]()
    ts.add(nonsiderealTarget("NST1"))
    ts.add(nonsiderealTarget("NST2"))
    ts
  }
}