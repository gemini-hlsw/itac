package edu.gemini.tac.psconversion

import edu.gemini.model.p1.{immutable => im}
import edu.gemini.tac.persistence.phase1.{Target => PsTarget}
import edu.gemini.tac.qengine.p1.Target
import edu.gemini.tac.qengine.p1.io.ObservationIo

import scalaz._


/**
 * Converts a Target into a persistence target.
 */
object TargetConverter {
  def MISSING_TARGET_DATA = "Missing target data"

  // downside, goes through several conversions: persistence -> mutable phase 1 -> immutable phase 1 -> queue engine
  // upside, don't have to rewrite target conversion code to short circuit that string of conversions

  def read(psTarget: PsTarget, when: Long): PsError \/ Target =
    for {
      m <- nullSafePersistence(MISSING_TARGET_DATA) { psTarget.toMutable }
    } yield ObservationIo.target(im.Target(m), when)
}