package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.util.Percent

object ConditionsBin {

  /**
   * Creates a list of ConditionsBin[T] from an array of (Category, T).
   */
  def list[T](bins: (ConditionsCategory, T)*): List[ConditionsBin[T]] =
    bins.map(tup => ConditionsBin(tup._1, tup._2)).toList

  def percentBins(bins: (ConditionsCategory, Double)*): List[ConditionsBin[Percent]] =
    bins.map(tup => ConditionsBin(tup._1, Percent(tup._2))).toList
}


/**
 * ConditionsBin associates a specification of ranges of observing conditions
 * with a fill percentage.
 */
final case class ConditionsBin[T](cat: ConditionsCategory, binValue: T) {
  def map[U](f: T => U): ConditionsBin[U] = new ConditionsBin(cat, f(binValue))
  def updated(newValue: T): ConditionsBin[T] = new ConditionsBin(cat, newValue)
}



