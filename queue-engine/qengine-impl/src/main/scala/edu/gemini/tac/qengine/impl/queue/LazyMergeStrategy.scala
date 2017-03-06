package edu.gemini.tac.qengine.impl.queue

import scala.collection.immutable.List
import edu.gemini.tac.qengine.p1.{JointProposal, Proposal, JointProposalPart}

/**
 * A merge strategy that keeps all the proposal parts in the queue until the
 * end, when the mergeParts method is called.  Adding a proposal is quick
 * because we don't attempt to merge it into any existing joint.
 */
object LazyMergeStrategy extends MergeStrategy {

  private class Span(
          val nonJoint: List[(Proposal, Double)],
          jointMap: Map[String, List[(Proposal, Double)]]) {

    def this() = this(Nil, Map.empty)

    def +(tup: (Proposal, Double)): Span =
      tup match {
        case (prop, index) if (prop.jointId.isDefined) => {
          val id = prop.jointId.get
          new Span(nonJoint, jointMap + (id -> ((prop, index) :: jointMap.getOrElse(id, Nil))))
        }
        case (prop, index) =>
          new Span(((prop, index) :: nonJoint), jointMap)
      }

    private def awarded(prop: Proposal): Double = prop.ntac.awardedTime.toHours.value

    private def totalHrs(lst: List[(Proposal, Double)]): Double =
      (0.0/:lst)((time, tup) => time + awarded(tup._1))

    private def weightedHrs(lst: List[(Proposal, Double)]): Double =
      lst.foldLeft(0.0) { (hrs, tup) => hrs + awarded(tup._1) * tup._2 }

    // Calculates the weighted index of the joint.
    private def jointIndex(lst: List[(Proposal, Double)]): Double =
      weightedHrs(lst) / totalHrs(lst)

    private def partList(props: List[Proposal]): List[JointProposalPart] =
      props.foldLeft(List.empty[JointProposalPart]) {
        (lst, prop) => lst ::: parts(prop)
      }

    // Maps the list of proposal parts into a single Joint proposal with a
    // weighted average index.
    private def mergeJoints(lst: List[(Proposal, Double)]): (JointProposal, Double) =
      (JointProposal.merge(partList(lst.unzip._1)), jointIndex(lst))

    // Converts the map id -> List[Part] into a map of id -> Joint
    private def mergeJoints: Map[String, (JointProposal, Double)] =
      jointMap map { kv => (kv._1 -> mergeJoints(kv._2))}

    // Append the non joints to the merged joints.  By putting them in this
    // order we prefer the original places of the non joint proposals over the
    // calculated index of the joint proposals just like the EagerMergeStrategy.
    private def unsortedMerge: List[(Proposal, Double)] =
      mergeJoints.values.toList ::: nonJoint

    // Merge and then sort by index, unzip to extract the proposals in order.
    def merge: List[Proposal] =
      unsortedMerge.sortWith((tup1, tup2) => tup1._2 < tup2._2).unzip._1
  }

  private def zipWithDoubleIndex(proposals: List[Proposal]): List[(Proposal, Double)] =
    proposals.zipWithIndex.map {
      case (proposal, index) => (proposal, index.toDouble)
    }

  // Merges all the parts into single joint proposals and sorts them into their
  // proper place in the list of proposals.
  def merge(proposals: List[Proposal]): List[Proposal] =
    zipWithDoubleIndex(proposals).foldLeft(new Span())(_ + _).merge

  // Just adds a proposal to the list.  Parts are merged later when required.
  def add(prop: Proposal, proposals: List[Proposal]): List[Proposal] =
    prop :: proposals
}