package edu.gemini.tac.service.check.impl.checks

import org.junit.Test
import org.junit.Assert._
import edu.gemini.tac.service.check.impl.core.ProposalIssue
import edu.gemini.tac.persistence.Proposal
import edu.gemini.tac.persistence.fixtures.builders.{ProposalBuilder, QueueProposalBuilder}
import edu.gemini.model.p1.mutable.CoordinatesEpoch
import edu.gemini.tac.service.check.impl.checks.{ZeroRaAndDecInNonTooCheck => Ck}
import edu.gemini.shared.skycalc.Angle
import edu.gemini.tac.persistence.phase1._
import java.util.List

class ZeroRaAndDecInNonTooCheckTest {
  @Test def justTooAreFine() {
    val p = justTooProposal(zero)
    assertEquals(ProposalIssue.noIssues, Ck(p))
  }

  @Test def nonZeroSiderealsAreFine(){
    val p = justSiderealProposal(nonZero)
    assertEquals(ProposalIssue.noIssues, Ck(p))
  }

  @Test def nonZeroNonsiderealsAreFine(){
    val p = justNonsiderealProposal(nonZero)
    assertEquals(ProposalIssue.noIssues, Ck(p))
  }

  @Test def warnOnSiderealZero(){
    val p = justSiderealProposal(zero)
    assertEquals(Ck.zeroTargets(p), Ck(p))
  }

  @Test def warnOnNonsiderealZero(){
    val p = justNonsiderealProposal(zero)
    assertEquals(Ck.zeroTargets(p), Ck(p))
  }

   private def proposal: Proposal = {
    val p = new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(ProposalBuilder.dummyPartnerUS).
      create();
    p
  }

  def justTooProposal(c : Coordinates) = {
    val p: Proposal = proposal
      // destroy beautifully valid proposal by removing the default target
    p.getPhaseIProposal.setTargets(twoToOTargets(c))
    p
  }

  def justSiderealProposal(c : Coordinates) = {
    val p = proposal
    p.getPhaseIProposal.setTargets(twoSiderealTargets(c))
    p
  }

  def justNonsiderealProposal(c : Coordinates) = {
    val p = proposal
    p.getPhaseIProposal.setTargets(twoNonsiderealTargets(c))
    p
  }

  def twoToOTargets(c : Coordinates) : java.util.ArrayList[Target] = {
    val ts = new java.util.ArrayList[Target]()
    ts.add(tooTarget("TooTarget1", c))
    ts.add(tooTarget("TooTarget2", c))
    ts
  }

  def tooTarget(nm : String, c : Coordinates) : Target = {
    val t = new TooTarget()
    t.setEpoch(CoordinatesEpoch.J_2000)
    t.setName(nm)
    t
  }

  def siderealTarget(nm : String, c : Coordinates) : Target = {
    val t = new SiderealTarget()
    t.setName(nm)
    t.setCoordinates(c)
    t
  }

  def nonsiderealTarget(nm : String, c : Coordinates) : Target = {
    val t = new NonsiderealTarget()
    t.setName(nm)
    val  ee = new EphemerisElement()
    ee.setCoordinates(c)
    val ees =  new java.util.ArrayList[EphemerisElement]()
    ees.add(ee)
    t.setEphemeris(ees)
    t
  }

  private val nonZero = new DegDegCoordinates(Angle.ANGLE_PI, Angle.ANGLE_PI)
  private val zero = new DegDegCoordinates(Angle.ANGLE_0DEGREES, Angle.ANGLE_0DEGREES)

  def twoSiderealTargets(c : Coordinates) : java.util.ArrayList[Target] = {
    val ts = new java.util.ArrayList[Target]()
    ts.add(siderealTarget("Sidereal1", c))
    ts.add(siderealTarget("Sidereal2", c))
    ts
  }

  def twoNonsiderealTargets(c : Coordinates) : java.util.ArrayList[Target] = {
    val ts = new java.util.ArrayList[Target]()
    ts.add(nonsiderealTarget("NST1", c))
    ts.add(nonsiderealTarget("NST2", c))
    ts
  }
}