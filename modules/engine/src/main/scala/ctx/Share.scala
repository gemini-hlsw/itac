// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.ctx

/**
 * Partner percentage share.  Rather than storing Double (which is problematic
 * for comparisons) we'll store an exact integer value.  57.7 % is 5700
 */
case class Share(value: Long) extends Ordered[Share] {
  def toPercentage: Double               = value / 100.0
  def toDouble: Double                   = value / 10000.0
  override def compare(that: Share): Int = value.compareTo(that.value)
}

object Share {
  def fromPercentage(perc: Double): Share = Share((perc * 100.0).round)
  def fromDouble(d: Double): Share        = Share((d * 10000.0).round)
}
