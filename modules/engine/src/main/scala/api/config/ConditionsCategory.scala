// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.p1._
import ConditionsCategory._
import scala.Ordering.Implicits._

/** Specification for a set of observing conditions. */
final case class ConditionsCategory(
  ccSpec: Specification[CloudCover]    = UnspecifiedCC,
  iqSpec: Specification[ImageQuality]  = UnspecifiedIQ,
  sbSpec: Specification[SkyBackground] = UnspecifiedSB,
  wvSpec: Specification[WaterVapor]    = UnspecifiedWV,
  name:   Option[String]               = None
) {

  def matches(oc: ObservingConditions): Boolean =
    ccSpec.matches(oc.cc) && iqSpec.matches(oc.iq) &&
      sbSpec.matches(oc.sb) && wvSpec.matches(oc.wv)

  def canObserve(oc: ObservingConditions): Boolean =
    ccSpec.canObserve(oc.cc) && iqSpec.canObserve(oc.iq) &&
      sbSpec.canObserve(oc.sb) && wvSpec.canObserve(oc.wv)

  override def toString: String =
    "%s (%5s, %6s, %6s)".format(name.getOrElse(""), iqSpec, ccSpec, sbSpec)

}

/** Defines valid specifications for observing conditions categories. */
object ConditionsCategory {

  /** A constraint on some observing condition. */
  sealed trait Specification[A <: ObservingCondition] { // N.B. upper bound is only for doc

    /**
     * Determines whether the given conditions variable matches (falls in) the
     * category.  For example, if the category is >=CC70 then CC70, CC80, and
     * CCAny match, but CC50 does not.
     */
    def matches(other: A): Boolean

    /**
     * Determines whether the given conditions could be observed if the current
     * conditions matched this category.
     */
    def canObserve(cond: A): Boolean

  }

  sealed abstract class Unspecified[A <: ObservingCondition] extends Specification[A] {
    override def matches(other: A): Boolean   = true
    override def canObserve(cond: A): Boolean = true
    override def toString: String             = "--"
  }

  object UnspecifiedCC extends Unspecified[CloudCover]
  object UnspecifiedIQ extends Unspecified[ImageQuality]
  object UnspecifiedSB extends Unspecified[SkyBackground]
  object UnspecifiedWV extends Unspecified[WaterVapor]

  case class Le[A <: ObservingCondition : Ordering](oc: A) extends Specification[A] {
    override def matches(other: A): Boolean = other <= oc
    // When the category is anything better than or equal to X, we are saying
    // that the better conditions are essentially the same. For example,
    // <=SB50 will be able to observe anything.  SB20 and SB50 of course but
    // also the conditions are good enough for anything worse.
    override def canObserve(other: A): Boolean = true
    override def toString: String              = "<=" + oc
  }

  case class Eq[A <: ObservingCondition : Ordering](oc: A) extends Specification[A] {
    override def matches(other: A): Boolean    = other == oc
    override def canObserve(other: A): Boolean = oc <= other
    override def toString: String              = oc.toString
  }

  case class Ge[A <: ObservingCondition : Ordering](oc: A) extends Specification[A] {
    override def matches(other: A): Boolean    = other >= oc
    override def canObserve(other: A): Boolean = oc <= other
    override def toString: String              = ">=" + oc
  }

  // RCN: I don't understand this.
  case class SearchPath(cats: List[ConditionsCategory]) {

    def apply(oc: ObservingConditions): List[ConditionsCategory] =
      cats.filter(_.canObserve(oc)).reverse // why?

    def category(oc: ObservingConditions): ConditionsCategory =
      cats.find(_.matches(oc)).get
  }

}
