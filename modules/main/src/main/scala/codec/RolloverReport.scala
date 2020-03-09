// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p2.rollover.RolloverObservation
import edu.gemini.tac.qengine.p2.rollover.RolloverReport
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._

trait RolloverReportCodec {
  import site._
  import semester._
  import rolloverobservatiom._

  implicit val encoderRolloverReport: Encoder[RolloverReport] = deriveEncoder

  def decoderRolloverReport(partners: List[Partner]): Decoder[RolloverReport] = {
    implicit val dro: Decoder[RolloverObservation] = decoderRolloverObservation(partners)
    (dro, ())._2 // defeat the unused checker, which thinks `dro` is unused
    deriveDecoder
  }

}

object rolloverreport extends RolloverReportCodec