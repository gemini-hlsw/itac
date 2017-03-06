package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._

import edu.gemini.tac.service.{check => api}

import edu.gemini.tac.persistence.joints.JointProposal
import edu.gemini.tac.service.check.impl.core.ProposalIssue
import IfJoint._
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}

class IfJointTest {

  private def warning(p: Proposal): api.ProposalIssue =
    ProposalIssue.warning(Band3MinimumTimeCheck, p, "xyz", ProposalIssueCategory.TimeAllocation)

  private def mkWarning(p: Proposal): Option[api.ProposalIssue] = Some(warning(p))
  private def mkWarningSet(p: Proposal): Set[api.ProposalIssue] = Set(warning(p))

  private def neverCalled: Option[api.ProposalIssue] =
    sys.error("should never be called")

  private def neverCalledSet: Set[api.ProposalIssue] =
    sys.error("should never be called")

  @Test def testIfJointWhenIsJoint() {
    val p = new JointProposal
    assertTrue(p.isJoint)

    val Some(issue) = ifJoint(p) { mkWarning(p) }
    assertEquals("xyz", issue.message)
  }

  @Test def testIfJointWhenNotIsJoint() {
    val p = new Proposal
    assertFalse(p.isJoint)

    ifJoint(p) { neverCalled } match {
      case None => // ok
      case _ => fail()
    }
  }

  @Test def testIfJointWhenIsJointWithSetResult() {
    val p = new JointProposal
    assertTrue(p.isJoint)

    val s = ifJoint(p) { mkWarningSet(p) }
    assertEquals(1, s.size)
    s foreach { issue => assertEquals("xyz", issue.message) }
  }

  @Test def testIfJointWhenNotIsJointWithSetResult() {
    val p = new Proposal
    assertFalse(p.isJoint)

    val s = ifJoint(p) { neverCalledSet }
    assertTrue(s.isEmpty)
  }

  @Test def testIfNotJointWhenIsJoint() {
    val p = new JointProposal
    assertTrue(p.isJoint)

    ifNotJoint(p) { neverCalled } match {
      case None => // ok
      case _ => fail()
    }
  }

  @Test def testIfNotJointWhenNotJoint() {
    val p = new Proposal
    assertFalse(p.isJoint)

    val Some(issue) = ifNotJoint(p) { mkWarning(p) }
    assertEquals("xyz", issue.message)
  }

  @Test def testIfJointComponentWhenJointComponent() {
    val p = new Proposal {
      override def isJointComponent: Boolean = true
    }

    val Some(issue) = ifJointComponent(p) { mkWarning(p) }
    assertEquals("xyz", issue.message)
  }

  @Test def testIfJointComponentWhenNotJointComponent() {
    val p = new JointProposal
    assertFalse(p.isJointComponent)

    ifJointComponent(p) { neverCalled } match {
      case None => // ok
      case _ => fail()
    }
  }

  @Test def testIfNotJointComponentWhenJointComponent() {
    val p = new Proposal {
      override def isJointComponent: Boolean = true
    }

    ifNotJointComponent(p) { neverCalled } match {
      case None => //ok
      case _ => fail()
    }
  }

  @Test def testIfNotJointComponentWhenNotJointComponent() {
    val p = new JointProposal
    assertFalse(p.isJointComponent)

    val Some(issue) = ifNotJointComponent(p) { mkWarning(p) }
    assertEquals("xyz", issue.message)
  }
}