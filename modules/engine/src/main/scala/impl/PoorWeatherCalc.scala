// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.impl

import edu.gemini.tac.qengine.p1.{JointProposalPart, JointProposal, Proposal}
import annotation.tailrec

/**
 * Encapsulates code for calculating the poor weather queue.
 */
object PoorWeatherCalc {

  @tailrec
  private def partition(
    rem: List[Proposal],
    parts: List[JointProposalPart],
    nonJoints: List[Proposal]
  ): (List[JointProposalPart], List[Proposal]) =
    rem match {
      case Nil =>
        (parts.reverse, nonJoints) // reverse to preserve the original order in JointProposals (for testing mainly)
      case (head: JointProposalPart) :: tail => partition(tail, head :: parts, nonJoints)
      case head :: tail                      => partition(tail, parts, head :: nonJoints)
    }

  private def joinParts(props: List[Proposal]): List[Proposal] = {
    val (parts, nonJoints) = partition(props, Nil, Nil)
    val joints             = JointProposal.mergeMatching(parts)
    joints ::: nonJoints
  }

  def apply(candidates: List[Proposal]): List[Proposal] =
    joinParts(Proposal.expandJoints(candidates).filter(_.isPoorWeather)).sortBy(_.id)
}
