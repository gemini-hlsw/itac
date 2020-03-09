package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.{check => api}

import edu.gemini.tac.persistence.Proposal

abstract class FalseResultType[+T] {
  def empty: T
}

object FalseResultType {
  implicit object EmptySet extends FalseResultType[Set[api.ProposalIssue]] {
    def empty = Set.empty
  }

  implicit object EmptyOption extends FalseResultType[Option[api.ProposalIssue]] {
    def empty = None
  }
}

/**
 * Some convenience for working with proposal checks that require
 * differentiating on proposal types (joint vs non-joint vs. joint component).
 * These are useful when the return type is an empty something when the
 * test fails.  If the situation requires computing some other value when the
 * test fails, then a normal if statement is required.
 */
object IfJoint {
  private def applyTest[T](p: Proposal, f: Proposal => Boolean, res: => T, ev: FalseResultType[T]): T =
    if (f(p)) res else ev.empty

  /**
   * Executes the by-name expression "res" if the proposal is a joint proposal,
   * returning the associated empty value otherwise.  In other words, returns
   * an empty result for non-joint proposals and joint components and computes
   * <code>res</code> for joint proposals.
   */
  def ifJoint[T](p: Proposal)(res: => T)(implicit ev: FalseResultType[T]): T =
    applyTest(p, _.isJoint, res, ev)

  /**
   * Executes the by-name expression "res" if the proposal is not a joint
   * proposal, returning the associated empty value otherwise.  In other words,
   * returns returns an empty result for joint proposals and computes <code>
   * res</code> for non-joints and joint components.
   */
  def ifNotJoint[T](p: Proposal)(res: => T)(implicit ev: FalseResultType[T]): T =
    applyTest(p, !_.isJoint, res, ev)

  /**
   * Executes the by-name expression "res" if the proposal is a joint proposal
   * component, returning the associated empty value otherwise.  In other words,
   * returns an empty result for joint and non-joint proposals and computes
   * <code>res</code> for joint components.
   */
  def ifJointComponent[T](p: Proposal)(res: => T)(implicit ev: FalseResultType[T]): T =
    applyTest(p, _.isJointComponent, res, ev)

  /**
   * Executes the by-name expression "res" if the proposal is not a joint
   * proposal component, returning the associated empty value otherwise.
   * In other words, returns an empty result for joint components and computes
   * <code>res</code> for joint and non-joint proposals.
   */
  def ifNotJointComponent[T](p: Proposal)(res: => T)(implicit ev: FalseResultType[T]): T =
    applyTest(p, !_.isJointComponent, res, ev)
}