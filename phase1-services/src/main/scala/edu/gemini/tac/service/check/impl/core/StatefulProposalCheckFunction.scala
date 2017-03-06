package edu.gemini.tac.service.check.impl.core

import edu.gemini.tac.service.{check => api}

import edu.gemini.tac.persistence.Proposal

/**
 * A trait for proposal check functions that require state information.  These
 * check functions work across a collection of proposals and need information
 * extracted from previous invocations in order to perform their task.  For
 * example, a proposal check that tests whether all proposal references are
 * unique across a collection of proposals needs to keep a set of
 * proposal references.
 */
trait StatefulProposalCheckFunction[S] extends ((Proposal, S) => (Set[api.ProposalIssue], S)) with BaseProposalCheckFunction {

  /**
   * Gets the initial value of the state.  For example, if a Set of
   * proposal references is being maintained, this will be an empty Set.
   */
  def initialState: S
}