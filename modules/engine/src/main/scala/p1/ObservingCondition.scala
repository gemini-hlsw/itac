// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.p1

/** Base type for all observing conditions. This might make more sense as a typeclass. */
sealed abstract class ObservingCondition(
  val prefix: String,
  val percent: Int
) {

  final override def toString: String =
    prefix + (if (percent < 100) percent else "Any")

}

sealed abstract class CloudCover(percent: Int) extends ObservingCondition("CC", percent)
object CloudCover {

  case object CC50  extends CloudCover(50)
  case object CC70  extends CloudCover(70)
  case object CC80  extends CloudCover(80)
  case object CCAny extends CloudCover(100)

  val values = List(CC50, CC70, CC80, CCAny)

  implicit val CloudCoverOrder: Ordering[CloudCover] =
    Ordering.by(_.percent)

}

sealed abstract class ImageQuality(percent: Int) extends ObservingCondition("IQ", percent)

object ImageQuality {

  case object IQ20  extends ImageQuality(20)
  case object IQ70  extends ImageQuality(70)
  case object IQ85  extends ImageQuality(85)
  case object IQAny extends ImageQuality(100)

  val values = List(IQ20, IQ70, IQ85, IQAny)

  implicit val ImageQualityOrder: Ordering[ImageQuality] =
    Ordering.by(_.percent)

}

sealed abstract class SkyBackground(percent: Int) extends ObservingCondition("SB", percent)

object SkyBackground {

  case object SB20  extends SkyBackground(20)
  case object SB50  extends SkyBackground(50)
  case object SB80  extends SkyBackground(80)
  case object SBAny extends SkyBackground(100)

  val values = List(SB20, SB50, SB80, SBAny)

  implicit val SkyBackgroundOrder: Ordering[SkyBackground] =
    Ordering.by(_.percent)

}

sealed abstract class WaterVapor(percent: Int) extends ObservingCondition("WV", percent)

object WaterVapor {

  case object WV20  extends WaterVapor(20)
  case object WV50  extends WaterVapor(50)
  case object WV80  extends WaterVapor(80)
  case object WVAny extends WaterVapor(100)

  val values = List(WV20, WV50, WV80, WVAny)

  implicit val WaterVaporOrder: Ordering[WaterVapor] =
    Ordering.by(_.percent)

}
