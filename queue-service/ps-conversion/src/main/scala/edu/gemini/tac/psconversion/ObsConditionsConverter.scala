package edu.gemini.tac.psconversion

import edu.gemini.model.p1.{immutable => im}
import edu.gemini.tac.persistence.phase1.{Condition => PsCondition}
import edu.gemini.tac.qengine.p1.ObsConditions
import edu.gemini.tac.qengine.p1.io.ObservationIo

import scalaz._

object ObsConditionsConverter {
  def read(psCondition: PsCondition): PsError \/ ObsConditions =
    for {
      m <- nullSafePersistence("Missing observation conditions") { psCondition.toPhase1 }
    } yield ObservationIo.conditions(im.Condition(m))
}