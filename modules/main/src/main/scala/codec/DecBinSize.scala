// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import io.circe._
import edu.gemini.qengine.skycalc.DecBinSize
import edu.gemini.qengine.skycalc.BinSizeException

trait DecBinSizeCodec {

  implicit val EncoderDecBinSize: Encoder[DecBinSize] =
    Encoder[Int].contramap(_.getSize)

  implicit val DecoderDecBinSize: Decoder[DecBinSize] =
    Decoder[Int].emap { n =>
      try {
        Right(new DecBinSize(n))
      } catch {
        case bse: BinSizeException => Left(bse.getMessage)
      }
    }

}

object Decbinsize extends DecBinSizeCodec
