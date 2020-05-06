// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.util

import cats.implicits._
import edu.gemini.tac.qengine.p1.Observation
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.p1.Target
import gsp.math.Angle
import gsp.math.Coordinates
import scala.collection.immutable.Queue
import cats.data.NonEmptyChain

/**
 *
 * @param tolerance Allowed tolerance in angular separation between target coordinates (default is
 * one arcsecond).
 */
class TargetDuplicationChecker(proposals: List[Proposal], val tolerance: Angle = Angle.arcseconds.reverseGet(10)) {

  // Sanity check
  assert(tolerance.toSignedDoubleDegrees >= 0.0, "Tolerance must be an angle within [0, 180)°")

  /** Get the gsp math coordinates from an engine model target. */
  def coordinates(target: Target): Coordinates =
    Coordinates.unsafeFromRadians(target.ra.toRad.mag, target.dec.toRad.mag)

  /** Are these the same coordinates within our tolerance? */
  def same(c1: Coordinates, c2: Coordinates): Boolean =
    (c1 angularDistance c2).toMicroarcseconds < tolerance.toMicroarcseconds

  /** Are these the same target within our tolerance? */
  def same(t1: Target, t2: Target): Boolean =
    same(coordinates(t1), coordinates(t2))

  /** Are these the same observation within our tolerance? */
  def same(o1: Observation, o2: Observation): Boolean =
    same(o1.target, o2.target)

  def same(ti1: TargetInfo, ti2: TargetInfo): Boolean =
    same(ti1.target, ti2.target)

  case class TargetInfo(proposalRef: String, target: Target)

  /**
   * A cluster of targets, grouped by NTAC reference, in which all edges in the minimal spanning
   * tree are <= `tolerance`.
   */
  type Cluster = Map[String, NonEmptyChain[Target]]

  /**
   * Find the members of the cluster that includes the `target`, taken from the list of candidates,
   * which should *not* contain `target`. Returns the cluster (which may only include `target`) and
   * a list of non-members.
   */
  def clusterIncluding(target: TargetInfo, candidates: List[TargetInfo]): (Cluster, List[TargetInfo]) = {
    // breadth-first search
    def go(queue: Queue[TargetInfo], members: List[TargetInfo], candidates: List[TargetInfo]): (Cluster, List[TargetInfo]) = {
      if (queue.isEmpty) {
        // This means there are no more nodes to expand, so the cluster is closed and we're done.
        val cluster = members.foldMap(ti => Map(ti.proposalRef -> NonEmptyChain(ti.target)))
        (cluster, candidates)
      } else {
        //
        val (focus, queueʹ) = queue.dequeue
        val (neighbors, candidatesʹ) = candidates.partition(same(focus, _))
        go(queueʹ ++ neighbors, focus :: members, candidatesʹ)
      }
    }
    go(Queue(target), Nil, candidates)
  }

  def allClusters: List[Map[String, NonEmptyChain[Target]]] = {

    def go(infos: List[TargetInfo], accum: List[Map[String, NonEmptyChain[Target]]] = Nil): List[Map[String, NonEmptyChain[Target]]] =
      infos match {
        case Nil    => accum
        case h :: t =>
          val (cluster, nonMembers) = clusterIncluding(h, t)
          go(nonMembers, if (cluster.size > 1) cluster :: accum else accum)
      }

    val infos: List[TargetInfo] =
      for {
        p <- proposals
        t <- (p.obsList ++ p.band3Observations).map(_.target).distinct
        if (t.ra.mag != 0 || t.dec.mag != 0)
      } yield TargetInfo(p.id.reference, t)

    go(infos)

  }

}