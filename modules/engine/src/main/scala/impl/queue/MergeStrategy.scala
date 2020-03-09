// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.impl.queue

import edu.gemini.tac.qengine.p1.{JointProposal, Proposal, JointProposalPart}

/**
 * Defines the strategy that the ProposalQueue uses for merging joint proposals.
 */
trait MergeStrategy {

  /**
   * Adds a proposal part to the list.
   */
  def add(prop: Proposal, proposals: List[Proposal]): List[Proposal]

  /**
   * Creates a proposal list in which all parts with the same joint id have
   * been merged.
   */
  def merge(proposals: List[Proposal]): List[Proposal]

  /**
   * Extracts a List of JointProposalPart from the proposal, which must be
   * either a JointProposal or a JointProposalPart.  If a NonJointProposal, an
   * error is thrown.
   */
  private[queue] def parts(p: Proposal): List[JointProposalPart] = p match {
    case joint: JointProposal    => joint.toParts
    case part: JointProposalPart => List(part)
    case _                       => sys.error("not expecting a non-joint proposal")
  }
}
