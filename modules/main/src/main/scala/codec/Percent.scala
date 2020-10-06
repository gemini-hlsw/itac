// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import io.circe._
import edu.gemini.tac.qengine.util.Percent

trait PercentCodec {

  implicit val EncoderPercent: Encoder[Percent] =
    Encoder[BigDecimal].contramap(_.value)

  implicit val DecoderPercent: Decoder[Percent] =
    Decoder[BigDecimal].map(Percent(_))

}

object percent extends PercentCodec