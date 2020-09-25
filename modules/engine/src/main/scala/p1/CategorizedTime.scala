package edu.gemini.tac.qengine.p1

import edu.gemini.tac.qengine.util.Time

/**
 * An amount of time categorized by target and conditions.
 */
trait CategorizedTime extends Product with Serializable {
  def target: Target
  def conditions: ObservingConditions
  def time: Time
}