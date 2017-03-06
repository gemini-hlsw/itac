package edu.gemini.tac.qservice.impl.queue

import edu.gemini.tac.persistence.bin.{DecBinSize => PsDecBinSize}
import edu.gemini.tac.psconversion._

import edu.gemini.qengine.skycalc.DecBinSize

import scalaz._
import Scalaz._

object DecBinSizeExtractor {
  def NEGATIVE_SIZE(amount: Int) = s"Negative Dec bin size ($amount) not supported"
  def UNSPECIFIED_SIZE           = "Unspecified Dec bin size degrees"
  def ILLEGAL_SIZE(deg: Int)     = s"Dec bin size $deg does not evenly divide 24 hours"

  def extract(decSize: PsDecBinSize): PsError \/ DecBinSize = {
    def extract(deg: Int): String \/ DecBinSize =
      if (deg < 0) NEGATIVE_SIZE(deg).left[DecBinSize]
      else \/.fromTryCatchNonFatal(new DecBinSize(deg)).leftMap(_ => ILLEGAL_SIZE(deg))

    for {
      deg  <- nullSafePersistence(UNSPECIFIED_SIZE) { decSize.getDegrees }
      size <- extract(deg.intValue).leftMap(BadData)
    } yield size
  }
}