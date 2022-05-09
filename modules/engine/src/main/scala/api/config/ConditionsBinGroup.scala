// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.p1.ObservingConditions
import edu.gemini.tac.qengine.util.Percent

/**
 * A mapping from conditions categories to values `A`, along with a search path that allows
 * observing conditions to be matched with categories with a preferred precedence.
 */
final case class ConditionsBinGroup[A](
  bins:       Map[ConditionsCategory, A],
  searchPath: ConditionsCategory.SearchPath
) {

  def updated(that: Seq[ConditionsBin[A]]): ConditionsBinGroup[A] =
    updated(that.map(bin => (bin.cat, bin.binValue)))

  def updated(that: Iterable[(ConditionsCategory, A)]): ConditionsBinGroup[A] = {
    require(
      that.foldLeft(true)((res, tup) => res && bins.get(tup._1).isDefined),
      "Cannot handle unknown categories."
    )
    new ConditionsBinGroup[A](bins ++ that, searchPath)
  }

  def updated(oc: ObservingConditions, newValue: A): ConditionsBinGroup[A] =
    updated(category(oc), newValue)

  def updated(c: ConditionsCategory, newValue: A): ConditionsBinGroup[A] = {
    require(bins.get(c).isDefined, "Cannot handle unknown categories: " + c.toString)
    new ConditionsBinGroup[A](bins.updated(c, newValue), searchPath)
  }

  def apply(c: ConditionsCategory): A =
    bins.get(c).getOrElse(sys.error(s"ConditionsBinGroup: no mapping for $c"))

  def map[B](f: A => B): ConditionsBinGroup[B] =
    new ConditionsBinGroup[B](bins.map { case (k,v) => (k, f(v)) }, searchPath)

  def category(oc: ObservingConditions): ConditionsCategory = searchPath.category(oc)

  /**
   * Gets the ordered list of ConditionsBin associated with the category into
   * which the given observing conditions fall.
   */
  def searchBins(oc: ObservingConditions): List[ConditionsBin[A]] =
    searchPath(oc).map(cat => ConditionsBin(cat, bins(cat)))

}

object ConditionsBinGroup {

  def of[A](bins: Seq[ConditionsBin[A]]): ConditionsBinGroup[A] = {
    // here we would sort and validate the sequence
    val map  = bins.map(bin => (bin.cat, bin.binValue)).toMap
    val path = ConditionsCategory.SearchPath(bins.map(_.cat).toList)
    new ConditionsBinGroup(map, path)
  }

  def ofPercent(bins: (ConditionsCategory, Double)*): ConditionsBinGroup[Percent] =
    of(ConditionsBin.ofPercent(bins: _*))

}
