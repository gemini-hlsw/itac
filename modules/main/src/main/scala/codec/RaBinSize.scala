// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import io.circe._
import edu.gemini.qengine.skycalc.RaBinSize
import edu.gemini.qengine.skycalc.BinSizeException

trait RaBinSizeCodec {

  implicit val EncoderRaBinSize: Encoder[RaBinSize] =
    Encoder[Int].contramap(_.getSize)

  implicit val DecoderRaBinSize: Decoder[RaBinSize] =
    Decoder[Int].emap { n =>
      try {
        Right(new RaBinSize(n))
      } catch {
        case bse: BinSizeException => Left(bse.getMessage)
      }
    }

}

object rabinsize extends RaBinSizeCodec
