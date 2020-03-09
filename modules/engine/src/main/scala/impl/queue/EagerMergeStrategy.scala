// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.impl.queue

import edu.gemini.tac.qengine.p1.{JointProposal, Proposal, JointProposalPart}

/**
 * A merge strategy that aways merges each joint proposal part as they are
 * added.  It is thought that this will generate a better queue because at any
 * given step, the current queue will be closer to the final queue after all
 * proposals are considered.
 */
case object EagerMergeStrategy extends MergeStrategy {

  // Keeps the parts always merged, so this is a no-op
  def merge(proposals: List[Proposal]): List[Proposal] = proposals

  private case class Res(joint: JointProposal, index: Double, merged: List[Proposal]) {
    def insert_?(count: Int): Boolean = index <= count && index > (count - 1)
    def prepend(prop: Proposal): Res  = Res(joint, index, prop :: merged)
    def insert(count: Int): Res       = if (insert_?(count)) prepend(joint) else this
  }

  private def toJoint(p: Proposal): JointProposal = {
    require(p.jointId.isDefined)
    p match {
      case joint: JointProposal    => joint
      case part: JointProposalPart => part.toJoint
      case _                       => sys.error("not expecting a non-joint proposal")
    }
  }

  private def merge(newProp: Proposal, oldProp: Proposal): JointProposal = {
    require(newProp.jointId == oldProp.jointId)
    JointProposal.merge(parts(oldProp) ::: parts(newProp))
  }

  private def calcIndex(newProp: Proposal, oldIndex: Int, oldProp: Proposal): Double = {
    val tOld = oldProp.ntac.awardedTime.toHours.value
    val tNew = newProp.ntac.awardedTime.toHours.value
    (oldIndex * tOld) / (tNew + tOld)
  }

  // Eagerly merges the part into the existing JointProposal, or else makes a
  // new JointProposal and puts it at the head of the list.
  private def add(p: Proposal, rem: List[Proposal], count: Int): Res =
    rem match {
      case Nil =>
        Res(toJoint(p), 0, Nil).insert(count)

      case oldProp :: tail if oldProp.jointId == p.jointId => {
        val newJoint = merge(p, oldProp)
        val newIndex = calcIndex(p, count, oldProp)
        Res(newJoint, newIndex, tail).insert(count)
      }

      case head :: tail =>
        add(p, tail, count + 1).prepend(head).insert(count)
    }

  def add(prop: Proposal, proposals: List[Proposal]): List[Proposal] =
    prop match {
      case prop: Proposal if (prop.jointId.isDefined) =>
        add(prop, proposals, 1).insert(0).merged
      case _ =>
        prop :: proposals
    }
}
