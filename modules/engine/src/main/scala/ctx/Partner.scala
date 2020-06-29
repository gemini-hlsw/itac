package edu.gemini.tac.qengine.ctx

import edu.gemini.tac.qengine.util.Percent
import edu.gemini.spModel.core.Site

/**
 * Phase 1 partner options.
 */
case class Partner(id: String, fullName: String, share: Percent, sites: Set[Site], email: String) {
  /**
   * Gets the partner's percentage share at the given site.
   */
  def percentAt(s: Site): Percent =
    if (sites.contains(s)) share else Percent.Zero

  def percentDoubleAt(s: Site): Double =
    percentAt(s).doubleValue

  override def toString: String = id

}

object Partner {
  val LargeProgramId = "LP"

  def apply(id: String, fullName: String, absPercent: Double, sites: Set[Site], email: String): Partner =
    Partner(id, fullName, Percent(absPercent), sites, email)

  /**
   * Creates a map with entries for all Partners according to the value
   * returned by the supplied function.
   */
  def mkMap[T](values: List[Partner], f: Partner => T): Map[Partner, T] =
    values.map(p => p -> f(p)).toMap

  // Completes the given PartialFunction by returning a default value for any
  // Partner for which pf is not defined.
  private def complete[T](pf: PartialFunction[Partner, T], default: T): Partner => T =
    p => pf.lift(p).getOrElse(default)

  /**
   * Creates a map with entries for all Partners according to the value
   * returned by the supplied partial function, using the supplied default
   * for any values not in pf's domain.
   */
  def mkMap[T](values: List[Partner], pf: PartialFunction[Partner, T], default: T): Map[Partner, T] =
    mkMap(values, complete(pf, default))
}