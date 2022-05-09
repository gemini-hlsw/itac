// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.util.Angle
import edu.gemini.tac.qengine.p1.Target


object RaBinGroup {

  // The total number of minutes in the full range of RAs.
  val TotalMin = 24*60

  def apply[T](bins: Seq[T]): RaBinGroup[T] = new RaBinGroup[T](bins.toIndexedSeq)

  /**
   * Generates an RaBinGroup given a bin size in minutes, which must evenly
   * divide the total number of minutes in 24 hours, and a function that
   * calculates a value based upon the angle corresponding to the center of
   * the bin and the size of the bin in minutes.
   */
  def gen[T ](binSizeMin: Int)(f: (Angle, Int) => T): RaBinGroup[T] = {
    require((TotalMin % binSizeMin) == 0)

    val r = 0 until TotalMin by binSizeMin
    val halfSize = binSizeMin/2.0
    new RaBinGroup(r.map(min => f(new Angle(min+halfSize, Angle.Min), binSizeMin)))
  }

  def gen15MinBins[T ] = gen[T](15)_
  def gen30MinBins[T ] = gen[T](30)_
  def gen45MinBins[T ] = gen[T](45)_
  def gen1HrBins[T ]   = gen[T](60)_
  def gen90MinBins[T ] = gen[T](90)_
  def gen2HrBins[T ]   = gen[T](120)_
  def gen3HrBins[T ]   = gen[T](180)_
  def gen4HrBins[T ]   = gen[T](240)_
}

import RaBinGroup._

// Choosing an IndexedSeq here because we want fast random access and arrays
// are complicated.

/**
 * An RaBinGroup is a parametrized collection indexed by RA angle.
 */
case class RaBinGroup[T ] private (val bins: IndexedSeq[T]) {
  require((TotalMin % bins.length) == 0)

  /** Size of each bin in minutes. */
  val sizeMin = TotalMin / bins.length

  def updated(ra: Angle, f: T => Option[T]): Option[RaBinGroup[T]] = {
    val i = indexOf(ra)
    f(bins(i)).map(bins.updated(i, _)).map(new RaBinGroup(_))
  }

  def updated(ra: Angle, t: T): RaBinGroup[T] = {
    new RaBinGroup(bins.updated(indexOf(ra), t))
  }

  def indexOf(ra: Angle): Int = ra.toPositive.toMin.mag.toInt / sizeMin

  def apply(min: Int): T  = bins((min % TotalMin) / sizeMin)
  def apply(ra: Angle): T  = bins(indexOf(ra))
  def apply(t: Target): T = apply(t.ra)

  def map[U](f: T => U): RaBinGroup[U] = new RaBinGroup[U](bins.map(f))
}

