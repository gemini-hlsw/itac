// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.p2.rollover

/**
 * An error in parsing the rollover report.
 */
case class RolloverParseError(reason: String)

object RolloverParseError {
  implicit val asRolloverParseError = (reason: String) => RolloverParseError(reason)
}
