// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import io.circe._
import io.circe.syntax._
import edu.gemini.tac.qengine.api.config.ConditionsBin
import edu.gemini.tac.qengine.api.config.ConditionsCategory

trait ConditionsBinCodec {
  import conditionscategory._

  implicit def EncoderConditionsBin[A: Encoder]: Encoder[ConditionsBin[A]] =
    Encoder.instance { b =>
      Json.obj(
        "conditions" -> b.cat.asJson,
        "name"       -> b.cat.name.asJson,
        "available"  -> b.binValue.asJson
      )
    }

  implicit def DecoderConditionsBin[A: Decoder]: Decoder[ConditionsBin[A]] =
    Decoder.instance { c =>
      for {
        cat  <- c.downField("conditions").as[ConditionsCategory]
        name <- c.downField("name").as[Option[String]]
        pct  <- c.downField("available").as[A]
      } yield ConditionsBin(cat.copy(name = name), pct)
    }

}

object conditionsbin extends ConditionsBinCodec
