package edu.gemini.tac.psconversion

import edu.gemini.tac.{persistence => ps}
import edu.gemini.tac.qengine.ctx.Semester

import java.text.ParseException
import scalaz._
import Scalaz._

object SemesterConverter {
  def UNSPECIFIED_SEMESTER: String = "Unspecified semester"
  def UNRECOGNIZED_SEMESTER(str: String): String = s"Unrecognized semester '$str'"

  def parse(s: String): BadData \/ Semester =
    try {
      Semester.parse(s).right
    } catch {
      case ex: ParseException => BadData(UNRECOGNIZED_SEMESTER(s)).left
    }

  def read(psSemester: ps.Semester): PsError \/ Semester =
    for {
      n <- nullSafePersistence(UNSPECIFIED_SEMESTER) { psSemester.getDisplayName }
      s <- parse(n)
    } yield s

  def read(psQueue: ps.queues.Queue): PsError \/ Semester =
    for {
      c  <- nullSafePersistence(UNSPECIFIED_SEMESTER) { psQueue.getCommittee }
      ps <- nullSafePersistence(UNSPECIFIED_SEMESTER) { c.getSemester }
      s  <- read(ps)
    } yield s
}
