package edu.gemini.tac.qengine.p2.rollover

/**
 * An error in parsing the rollover report.
 */
case class RolloverParseError(reason: String)

object RolloverParseError {
  implicit val asRolloverParseError = (reason: String) => RolloverParseError(reason)
}
