package edu.gemini.tac.qengine.p2

import edu.gemini.tac.qengine.p1.Mode
import edu.gemini.tac.qengine.util.CompoundOrdering
import edu.gemini.tac.qengine.ctx.{ContextOrderingImplicits, Semester, Site}

/**
 * Gemini science program id.
 */
case class ProgramId(site: Site, semester: Semester, mode: Mode, index: Int) extends Ordered[ProgramId] {
  require(index > 0)

  import ProgramId._

  def compare(that: ProgramId): Int = ProgramIdOrdering.compare(this, that)

  override def toString: String =
    "%s-%s-%s-%d".format(siteToString(site), semester.toString, mode.programId, index)
}

object ProgramId {
  import ContextOrderingImplicits._

  object ProgramIdOrdering extends CompoundOrdering(
    Ordering.by[ProgramId, Site](_.site),
    Ordering.by[ProgramId, Semester](_.semester),
    Ordering.by[ProgramId, Mode](_.mode),
    Ordering.by[ProgramId, Int](_.index))

  private def siteToString(s: Site): String = s.abbreviation.toUpperCase

  val Pattern = """(G[NS])-(\d\d\d\d[AB])-([CQ]|LP)-(\d+)"""
  val RegEx   = Pattern.r

  def parse(s: String): Option[ProgramId] =
    s match {
      case RegEx(siteStr, semesterStr, modeStr, indexStr) =>
        val site     = Site.parse(siteStr)
        val semester = Semester.parse(semesterStr)
        val mode     = Mode.parse(modeStr).get
        val index    = indexStr.toInt
        Some(ProgramId(site, semester, mode, index))
      case _ =>
        None
    }
}
