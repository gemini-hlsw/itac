// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.config

import io.circe._
import io.circe.generic.semiauto._

final case class PartnerConfig(
  email:   Email,
  percent: Percent,
  sites:   List[Site]
)

object PartnerConfig {
  import itac.codec.percent._
  import itac.codec.site._

  implicit val encoderPartnerConfig: Encoder[PartnerConfig] = deriveEncoder
  implicit val decoderPartnerConfig: Decoder[PartnerConfig] = deriveDecoder
}
