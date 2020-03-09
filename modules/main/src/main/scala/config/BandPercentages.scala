// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.config

import cats.implicits._
import io.circe._
import io.circe.syntax._

sealed abstract case class BandPercentages(
  band1: Percent,
  band2: Percent,
  band3: Percent
) {

  // sanity check; should be correct on construction
  assert(band1 + band2 + band3 == Percent.Hundred)

  object engine {
    import edu.gemini.tac.qengine.api.config.QueueBandPercentages
    def queueBandPercentages: QueueBandPercentages =
      QueueBandPercentages(band1, band2, band3)
  }

}

object BandPercentages {
  import itac.codec.percent._

  def apply(
    band1: Percent,
    band2: Percent,
    band3: Percent
  ): Either[String, BandPercentages] =
    if (band1 + band2 + band3 == Percent.Hundred) Right(new BandPercentages(band1, band2, band3) {})
    else Left(s"Band percentages do not add up to 100%: $band1 $band2 $band3")

  implicit val EncodeeBandPercentages: Encoder[BandPercentages] =
    Encoder.instance { p =>
      Json.obj(
        "band1" -> p.band1.asJson,
        "band2" -> p.band2.asJson,
        "band3" -> p.band3.asJson
      )
    }

  implicit val DecodeBandPercentages: Decoder[BandPercentages] =
    Decoder.instance { c =>
      (c.downField("band1").as[Percent],
       c.downField("band2").as[Percent],
       c.downField("band3").as[Percent]).tupled
    } .emap { case (b1, b2, b3) => apply(b1, b2, b3) }

}