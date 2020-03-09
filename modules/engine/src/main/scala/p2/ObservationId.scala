// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.p2

import edu.gemini.tac.qengine.p1.Mode
import edu.gemini.tac.qengine.util.CompoundOrdering
import edu.gemini.spModel.core.{ Semester, Site }

/**
 * Gemini science program observation id.
 */
case class ObservationId(progId: ProgramId, index: Int) extends Ordered[ObservationId] {
  require(index > 0)

  import ObservationId._

  def site: Site         = progId.site
  def semester: Semester = progId.semester
  def mode: Mode         = progId.mode
  def progIndex: Int     = progId.index

  def compare(that: ObservationId): Int =
    ObservationIdOrdering.compare(this, that)

  override def toString: String = "%s-%d".format(progId.toString, index)
}

object ObservationId {
  object ObservationIdOrdering
      extends CompoundOrdering(
        Ordering.by[ObservationId, ProgramId](_.progId),
        Ordering.by[ObservationId, Int](_.index)
      )

  val Pattern = """^(.*)-(\d+)$""".format(ProgramId.Pattern)
  val RegEx   = Pattern.r

  def parse(s: String): Option[ObservationId] =
    s match {
      case RegEx(progIdStr, obsIndexStr) =>
        val obsIndex = obsIndexStr.toInt
        ProgramId.parse(progIdStr).map { progId =>
          ObservationId(progId, obsIndex)
        }
      case _ =>
        None
    }
}
