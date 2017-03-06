package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.p1._
import ConditionsCategory._

/**
 * Defines valid specifications for observing conditions categories.
 */
object ConditionsCategory {
  trait Spec[V <: Var[V]] {
    /**
     * Determines whether the given conditions variable matches (falls in) the
     * category.  For example, if the category is >=CC70 then CC70, CC80, and
     * CCAny match, but CC50 does not.
     */
    def matches(other: V): Boolean

    /**
     * Determines whether the given conditions could be observed if the current
     * conditions matched this category.
     */
    def canObserve(cond: V): Boolean
  }

  private[ConditionsCategory] class Unspecified[V <: Var[V]]() extends Spec[V] {
    override def matches(other: V): Boolean   = true
    override def canObserve(cond: V): Boolean = true
    override def toString: String = "--"
  }

  object UnspecifiedCC extends Unspecified[CloudCover]
  object UnspecifiedIQ extends Unspecified[ImageQuality]
  object UnspecifiedSB extends Unspecified[SkyBackground]
  object UnspecifiedWV extends Unspecified[WaterVapor]

  case class Le[V <: Var[V]](ocVar: V) extends Spec[V] {
    override def matches(other: V): Boolean = other <= ocVar
    // When the category is anything better than or equal to X, we are saying
    // that the better conditions are essentially the same. For example,
    // <=SB50 will be able to observe anything.  SB20 and SB50 of course but
    // also the conditions are good enough for anything worse.
    override def canObserve(other: V): Boolean = true
    override def toString: String = "<=" + ocVar
  }

  case class Eq[V <: Var[V]](ocVar: V) extends Spec[V] {
    override def matches(other: V): Boolean = other == ocVar
    override def canObserve(other: V): Boolean = ocVar <= other
    override def toString: String = ocVar.toString
  }

  case class Ge[V <: Var[V]](ocVar: V) extends Spec[V] {
    override def matches(other: V): Boolean = other >= ocVar
    override def canObserve(other: V): Boolean = ocVar <= other
    override def toString: String = ">=" + ocVar
  }

  object SearchPath {
    /**
     * Creates a CategorySearchPath from the given list of conditions bins.
     * WARNING: assumes bins are sorted.
     */
    def apply(sortedBins: Seq[ConditionsBin[_]]): SearchPath = {
      new SearchPath(sortedBins.map(_.cat).toList)
    }
  }

  case class SearchPath(cats: List[ConditionsCategory]) {
    def apply(oc: ObsConditions): List[ConditionsCategory] =
      (List.empty[ConditionsCategory]/:cats) {
        (upgradePath, cat) => if (cat.canObserve(oc)) cat :: upgradePath else upgradePath
      }

    def category(oc: ObsConditions): ConditionsCategory =
      cats.find(_.matches(oc)).get
  }
}

/**
 * Conditions categories define a range of observing conditions.  A particular
 * collection of conditions variables will fall into the range or not.
 */
final case class ConditionsCategory(
        ccSpec: Spec[CloudCover]    = UnspecifiedCC,
        iqSpec: Spec[ImageQuality]  = UnspecifiedIQ,
        sbSpec: Spec[SkyBackground] = UnspecifiedSB,
        wvSpec: Spec[WaterVapor]    = UnspecifiedWV,
        name: Option[String]        = None) {

  def matches(oc: ObsConditions): Boolean =
    ccSpec.matches(oc.cc) && iqSpec.matches(oc.iq) &&
    sbSpec.matches(oc.sb) && wvSpec.matches(oc.wv)

  def canObserve(oc: ObsConditions): Boolean =
    ccSpec.canObserve(oc.cc) && iqSpec.canObserve(oc.iq) &&
    sbSpec.canObserve(oc.sb) && wvSpec.canObserve(oc.wv)

  override def toString: String =
    "%s (%5s, %6s, %6s)".format(name.getOrElse(""), iqSpec, ccSpec, sbSpec)
}
