package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.api.config.{ConditionsCategory => Cat}
import edu.gemini.tac.qengine.p1.ObsConditions
import edu.gemini.tac.qengine.util.Percent

object ConditionsBinGroup {
  def apply[T](bins: Seq[ConditionsBin[T]]): ConditionsBinGroup[T] = {
    // here we would sort and validate the sequence
    val map  = bins.map(bin => (bin.cat, bin.binValue)).toMap
    val path = Cat.SearchPath(bins)
    new ConditionsBinGroup(map, path)
  }

  def percentBins(bins: (Cat, Double)*): ConditionsBinGroup[Percent] =
    apply(ConditionsBin.percentBins(bins: _*))
}

class ConditionsBinGroup[T] private (val bins: Map[Cat, T], val searchPath: Cat.SearchPath) {

  def updated(that: Seq[ConditionsBin[T]]): ConditionsBinGroup[T] =
    updated(that.map(bin => (bin.cat, bin.binValue)))

  def updated(that: TraversableOnce[(Cat, T)]): ConditionsBinGroup[T] = {
    require((true/:that)((res, tup) => res && bins.get(tup._1).isDefined), "Cannot handle unknown categories.")
    new ConditionsBinGroup[T](bins ++ that, searchPath)
  }

  def updated(oc: ObsConditions, newValue: T): ConditionsBinGroup[T] =
    updated(category(oc), newValue)

  def updated(c: Cat, newValue: T): ConditionsBinGroup[T] = {
    require(bins.get(c).isDefined, "Cannot handle unknown categories: "+ c.toString)
    new ConditionsBinGroup[T](bins.updated(c, newValue), searchPath)
  }

  def apply(c: Cat): T = bins(c)

  def map[U](f: T => U): ConditionsBinGroup[U] =
    new ConditionsBinGroup[U](bins.mapValues(f(_)), searchPath)

  def category(oc: ObsConditions): ConditionsCategory = searchPath.category(oc)

  /**
   * Gets the ordered list of ConditionsBin associated with the category into
   * which the given observing conditions fall.
   */
  def searchBins(oc: ObsConditions): List[ConditionsBin[T]] =
    searchPath(oc).map(cat => ConditionsBin(cat, bins(cat)))

  def toXML = <ConditionsBinGroup>
    <!-- complexity -->
    </ConditionsBinGroup>
}
