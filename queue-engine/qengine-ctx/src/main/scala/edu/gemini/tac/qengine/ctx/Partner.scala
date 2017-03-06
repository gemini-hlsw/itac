package edu.gemini.tac.qengine.ctx

import xml.Elem
import edu.gemini.tac.qengine.util.Percent


//object Partner {
//
//  // Wraps a value containing a Set of both sites.  This is a bit odd, and I'm
//  // not sure why it was needed, but a simple private val (not wrapped in a
//  // singleton) would not work.
//  private object Sites {
//    val Both = Set(Site.north, Site.south)
//  }
//
//  //Forward to the Partners instance
//  def parse(s : String) = partners.parse(s)
//
//  def mkMap[T](f: Partner => T) =  partners.mkMap(f)
//
//  def mkMap[T](pf: PartialFunction[Partner, T], default: T) = partners.mkMap(pf, default)
//}

/**
 * Phase 1 partner options.
 */
case class Partner(id: String, fullName: String, share: Percent, sites: Set[Site]) {
  /**
   * Gets the partner's percentage share at the given site.
   */
  def percentAt(s: Site): Double = if (sites.contains(s)) share.doubleValue else 0.0

  override def toString: String = id

  def toXML : Elem = <Partner id={id}>
      <fullName>{fullName}</fullName>
      <absPercent>{"%.2f".format(share.doubleValue)}</absPercent>
      <sites>
        { sites.map{ s =>
            <Site>{s.abbreviation()}</Site>
          }
        }
      </sites>
    </Partner>
}

object Partner {
  val LargeProgramId = "LP"

  def apply(id: String, fullName: String, absPercent: Double, sites: Set[Site]): Partner =
    Partner(id, fullName, Percent(absPercent), sites)

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