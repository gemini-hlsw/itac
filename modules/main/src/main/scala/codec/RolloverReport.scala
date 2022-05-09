// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import edu.gemini.tac.qengine.p2.rollover.RolloverReport
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._

trait RolloverReportCodec {
  import site._
  import semester._
  import rolloverobservatiom._

  implicit val encoderRolloverReport: Encoder[RolloverReport] = deriveEncoder
  implicit val decoderRolloverReport: Decoder[RolloverReport] = deriveDecoder

}

object rolloverreport extends RolloverReportCodec