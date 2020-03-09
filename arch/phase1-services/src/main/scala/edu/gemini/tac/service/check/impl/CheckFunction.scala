package edu.gemini.tac.service.check.impl

import edu.gemini.tac.service.{check => api}
import core.{StatefulProposalCheckFunction, StatelessProposalCheckFunction}

import edu.gemini.tac.persistence.Proposal

/**
 * Adapts the StatelessProposalCheckFunction and the
 * StatefulProposalCheckFunction to the same interface for the ProposalChecker
 */
private[impl] trait CheckFunction extends api.ProposalCheck {
  val f: api.ProposalCheck
  def name: String        = f.name
  def description: String = f.description

  type Result = (Set[api.ProposalIssue], CheckFunction)
  def check(p: Proposal): Result
}

private[impl] object CheckFunction {
  case class StatelessAdapter(f: StatelessProposalCheckFunction) extends CheckFunction {
    def check(p: Proposal): Result = (f(p), this)
  }

  implicit val toAdapter:StatelessProposalCheckFunction => CheckFunction = (f: StatelessProposalCheckFunction) => StatelessAdapter(f)

  case class StatefulAdapter[S](f: StatefulProposalCheckFunction[S], state: S) extends CheckFunction {
    def check(p: Proposal): Result =
      f(p, state) match {
        case (issues, newState) => (issues, new StatefulAdapter[S](f, newState))
      }
  }

  object StatefulAdapter {
    def apply[S](f: StatefulProposalCheckFunction[S]): StatefulAdapter[S] =
      new StatefulAdapter(f, f.initialState)
  }

  import scala.language.implicitConversions

  implicit def toAdapter[S](f: StatefulProposalCheckFunction[S]): CheckFunction = StatefulAdapter(f)
}