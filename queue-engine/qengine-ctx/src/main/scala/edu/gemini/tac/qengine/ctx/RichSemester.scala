package edu.gemini.tac.qengine.ctx

/**
 * Adds Ordered features to Semester.
 */
final class RichSemester(val semester: Semester) extends Proxy with Ordered[Semester] {

  // Proxy
  def self: Any = semester

  // Ordered[Semester]
  def compare(that: Semester): Int = semester.compareTo(that)

  def min(that: Semester): Semester = if (semester.compareTo(that) < 0) semester else that
  def max(that: Semester): Semester = if (semester.compareTo(that) > 0) semester else that
}