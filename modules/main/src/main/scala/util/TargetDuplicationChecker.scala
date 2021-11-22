// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
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
import edu.gemini.model.p1.immutable.Investigator
import edu.gemini.model.p1.immutable.BlueprintBase
import edu.gemini.model.p1.immutable.VisitorBlueprint

/**
 *
 * @param tolerance Allowed tolerance in angular separation between target coordinates (default is
 * one arcsecond).
 */
class TargetDuplicationChecker(proposals: List[Proposal], val tolerance: Angle = Angle.arcseconds.reverseGet(10)) {
  import TargetDuplicationChecker._

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

  case class TargetInfo(proposalRef: String, pi: Investigator, target: Target, instrument: String) {
    def key: ClusterKey = ClusterKey(proposalRef, pi)
    def member: ClusterMember = ClusterMember(target, instrument)
  }

  /**
   * A cluster of targets, grouped by NTAC reference, in which all edges in the minimal spanning
   * tree are <= `tolerance`.
   */
  type Cluster = Map[ClusterKey, NonEmptyChain[ClusterMember]]

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
        val cluster = members.foldMap(ti => Map(ti.key -> NonEmptyChain(ti.member)))
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

  def allClusters: List[Cluster] = {

    def go(infos: List[TargetInfo], accum: List[Cluster] = Nil): List[Cluster] =
      infos match {
        case Nil    => accum
        case h :: t =>
          val (cluster, nonMembers) = clusterIncluding(h, t)
          go(nonMembers, if (cluster.size > 1) cluster :: accum else accum)
      }

    // Work around the case where some people use the actual Vistor blueprint instead of the real
    // blueprint for MAROON-X and IGRINS (and who knows what else in the future?)
    def blueprintName(bb: BlueprintBase): String =
      bb match {
        case v: VisitorBlueprint =>
          v.customName match {
            case "IGRINS - continuous observation" => "IGRINS"
            case other => other
          }
        case other => other.name
      }

    val infos: List[TargetInfo] =
      for {
        p <- proposals
        x <- (p.obsList ++ p.band3Observations).map(o => (o.target, o.p1Observation.blueprint.foldMap(blueprintName))).distinct
        (t, b) = x
        if (t.ra.mag != 0 || t.dec.mag != 0)
      } yield TargetInfo(p.id.reference, p.p1proposal.investigators.pi, t, b)

    // infos.map(_.instrument).distinct.sorted.foreach(println)

    // Compute clusters and filter out those where they all have the same PI email.
    // REL-3953 also filter out clusters where the instruments differ
    go(infos)
      .filterNot(_.keySet.map(_.pi.email).size == 1)
      .filterNot(_.values.toList.foldMap(_.toList).distinctBy(_.instrument).size > 1)

  }

}

object TargetDuplicationChecker {
  final case class ClusterKey(reference: String, pi: Investigator)
  final case class ClusterMember(target: Target, instrument: String) // !
}