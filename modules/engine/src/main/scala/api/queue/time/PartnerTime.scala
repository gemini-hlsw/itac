// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.queue.time

import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.util.Percent

final case class PartnerTime(
  band1: Time,
  band2: Time,
  band3: Time
) {

  def +(other: PartnerTime): PartnerTime =
    PartnerTime(
      band1 + other.band1,
      band2 + other.band2,
      band3 + other.band3
    )

  def -(other: PartnerTime): PartnerTime =
    PartnerTime(
      band1 - other.band1,
      band2 - other.band2,
      band3 - other.band3
    )

  def *(pct: Percent): PartnerTime =
    PartnerTime(
      band1 * pct,
      band2 * pct,
      band3 * pct
    )

  def nonNegative: PartnerTime =
    PartnerTime(
      band1.nonNegative,
      band2.nonNegative,
      band3.nonNegative
    )

  override def toString: String =
    s"PartnerTime($band1, $band2, $band3)"

}

object PartnerTime {

  val Zero: PartnerTime =
    PartnerTime(Time.Zero, Time.Zero, Time.Zero)

}