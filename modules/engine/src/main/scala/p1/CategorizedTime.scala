// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.p1

import edu.gemini.tac.qengine.util.Time

/**
 * An amount of time categorized by target and conditions.
 */
trait CategorizedTime {
  def target: Target
  def conditions: ObservingConditions
  def time: Time
}
