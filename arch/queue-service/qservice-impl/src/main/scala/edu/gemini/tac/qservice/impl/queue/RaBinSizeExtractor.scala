package edu.gemini.tac.qservice.impl.queue

import edu.gemini.tac.persistence.bin.{RABinSize => PsRaBinSize}
import edu.gemini.tac.psconversion._

import edu.gemini.qengine.skycalc.RaBinSize

import scalaz._
import Scalaz._


object RaBinSizeExtractor {
  def NEGATIVE_SIZE(amount: Int)  = s"Negative RA bin size ($amount) not supported"
  def UNSPECIFIED_SIZE_HOURS      = "Unspecified RA bin size hours"
  def UNSPECIFIED_SIZE_MINUTES    = "Unspecified RA bin size minutes"
  def ILLEGAL_SIZE(min: Int)      = s"RA bin size $min does not evenly divide 24 hours"

  def extract(raSize: PsRaBinSize): PsError \/ RaBinSize = {
    def extract(hours: Int, minutes: Int): String \/ RaBinSize =
      if (hours < 0) NEGATIVE_SIZE(hours).left[RaBinSize]
      else if (minutes < 0) NEGATIVE_SIZE(minutes).left[RaBinSize]
      else \/.fromTryCatchNonFatal(new RaBinSize(hours * 60 + minutes)).leftMap(_ => ILLEGAL_SIZE(hours*60+minutes))

    for {
      hours <- nullSafePersistence(UNSPECIFIED_SIZE_HOURS)   { raSize.getHours }
      mins  <- nullSafePersistence(UNSPECIFIED_SIZE_MINUTES) { raSize.getMinutes }
      size  <- extract(hours.intValue, mins.intValue).leftMap(BadData)
    } yield size
  }
}