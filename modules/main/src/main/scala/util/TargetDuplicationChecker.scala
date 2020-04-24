package itac.util

import cats.data.NonEmptyList
import cats.implicits._
import edu.gemini.tac.qengine.p1.Observation
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.p1.Target
import gsp.math.Angle
import gsp.math.Coordinates
import itac.ObservationDigest

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

  /**
   * Given a reference target and a list of proposals, return a list of pairs of proposals and the
   * digests of their matching observations.
   */
  def duplicates(referenceTarget: Target): List[(Proposal, NonEmptyList[String])] =
    proposals.flatMap { p =>

      // Digests of all observations in `p` that are close to `referenceTarget`.
      val duplicateDigests: List[String] =
        (p.obsList ++ p.band3Observations).flatMap { o =>
          if (same(referenceTarget, o.target)) List(ObservationDigest.digest(o.p1Observation))
          else Nil
        }

      // Only yield a value if there are duplicates.
      NonEmptyList
        .fromList(duplicateDigests)
        .foldMap(nel => List((p, nel)))

    }

  // As we check for duplicates we accumulate a set of observation digests that have already been
  // found to identify duplicates, as well as references targets that have already been examined.
  // We don't need to look at these again. The algorithm is O(n^2) and this optimization cuts the
  // search space in half in the worst case, more when there are duplicates.
  //
  // The set of duplicates for a given target can be discarded if only one program mentioned (this
  // means that the target appears more than once in the same program, which is fine).

  def duplicates: List[List[(Proposal, NonEmptyList[String])]] = {

    // All observations, minus those with dummy targets.
    val observations: List[Observation] =
      proposals
        .flatMap(p => p.obsList ++ p.band3Observations)
        .filter(o => o.target.ra.mag != 0.0 || o.target.dec.mag != 0.0)

    // As we check for duplicates we accumulate a set of observation digests that have already been
    // found to identify duplicates, as well as references targets that have already been examined.
    // We don't need to look at these again. The algorithm is O(n^2) and this optimization cuts the
    // search space in half in the worst case, more when there are duplicates.
    val initialState = (Set.empty[String], List.empty[List[(Proposal, NonEmptyList[String])]])
    observations.foldLeft(initialState) { case ((seen, accum), o) =>

      // ignore seen for now

      (seen, duplicates(o.target) :: accum)

    } ._2.filter(_.length > 1).distinct

  }


}