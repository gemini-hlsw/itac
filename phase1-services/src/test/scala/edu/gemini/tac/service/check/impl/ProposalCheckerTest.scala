package edu.gemini.tac.service.check.impl

import core.{StatelessProposalCheckFunction, ProposalIssue, StatefulProposalCheckFunction}
import edu.gemini.tac.service.{check => api}
import org.junit._
import Assert._

import edu.gemini.tac.persistence.joints.JointProposal

import collection.JavaConverters._
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}

// Tests the mechanics of the proposal checker itself, not any particular
// proposal check.

class ProposalCheckerTest {

  @Test def testEmptyProposalList() {
    val props = new java.util.ArrayList[Proposal]()
    assertEquals(0, ProposalChecker.exec(props).size)
  }

  // A check function that flags an issue for every proposal once it has seen
  // the first 3.  In other words, the 4th, 5th, ... are all flagged with an
  // issue.  Requires keeping up with state.
  object Accept3 extends StatefulProposalCheckFunction[Int] {
    def name         = "Accepts Only 3 Proposals"
    def description  = "Flags all proposals with errors after the first 3."
    def initialState = 1

    def msg(n: Int) = "proposal %d".format(n)

    def apply(p: Proposal, s: Int): (Set[api.ProposalIssue], Int) =
      s match {
        case n if n < 4 => (Set.empty, s+1)
        case n => (Set(ProposalIssue.error(this, p, msg(n), ProposalIssueCategory.Structural)), s)
      }
  }

  // A check function that flags an issue for all proposals with even ids.
  // This determination doesn't require any state.
  object EvenIds extends StatelessProposalCheckFunction {
    def name         = "Even Proposal Ids"
    def description  = "Flags proposal with even ids as"
    def msg(l: Long) = "Has even proposal id %d".format(l)

    def apply(p: Proposal): Set[api.ProposalIssue] =
      p.getId.longValue match {
        case n if n % 2 == 0 => Set(ProposalIssue.error(this, p, msg(n), ProposalIssueCategory.Structural))
        case _               => Set.empty
      }
  }

  private def mkProps(range: Range): List[Proposal] =
    range.map {
      n => new Proposal {
        override def getId: java.lang.Long = n
        override def equals(o: Any): Boolean = o match {
          case p: Proposal => getId.longValue == p.getId.longValue
          case _ => false
        }
        override def hashCode: Int = getId.hashCode
      }
    }.toList

  @Test def testStatefulFunction() {
    val checks = List[CheckFunction](Accept3)

    // The first time through, we only give it 3 proposals so it won't trigger
    // an issue.
    val props3 = List(new Proposal, new Proposal, new Proposal)
    val fv3 = ProposalChecker.exec(props3, checks)
    assertEquals(0, fv3.issues.size)

    val props4 = new Proposal :: props3
    val fv4 = ProposalChecker.exec(props4, checks)
    assertEquals(1, fv4.issues.size)
    assertEquals(Accept3.msg(4), fv4.issues.head.message)
  }

  @Test def testStatelessFunction() {
    val checks = List[CheckFunction](EvenIds)

    // Should flag errors for the two proposals with even ids
    val props3 = mkProps(0 to 3)
    val fv3 = ProposalChecker.exec(props3, checks)
    assertEquals(2, fv3.issues.size)
    assertEquals(Set(0, 2), fv3.issues.map(_.proposal.getId))
  }

  @Test def testMultipleChecks() {
    val checks = List[CheckFunction](Accept3, EvenIds)

    val props4 = mkProps(1 to 4)
    val fv = ProposalChecker.exec(props4, checks)

    // Expect an "accept 3" error for proposal with id 4
    val issue0 = ProposalIssue.error(Accept3, props4.last, Accept3.msg(4), ProposalIssueCategory.Structural)

    // Expect issues for propoals with ids 2 and 4
    val issue1 = ProposalIssue.error(EvenIds, props4.drop(1).head, EvenIds.msg(2), ProposalIssueCategory.Structural)
    val issue2 = ProposalIssue.error(EvenIds, props4.last,         EvenIds.msg(4), ProposalIssueCategory.Structural)

    // Expect all these issues and not any others
    val expected = Set(issue0, issue1, issue2)
    assertEquals(expected, fv.issues)
  }
}