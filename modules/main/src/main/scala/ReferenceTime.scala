// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

/**
 * A reference time to use when determining target coordinates. Nonsidereal target edits can specify
 * a non-default reference time via an edit.
 */
sealed trait ReferenceTime {
  import ReferenceTime._
  def getOrElse(default: Long): Long =
    this match {
      case Default  => default
      case At(when) => when
    }
}
object ReferenceTime {
  final case object Default        extends ReferenceTime
  final case class  At(when: Long) extends ReferenceTime
}