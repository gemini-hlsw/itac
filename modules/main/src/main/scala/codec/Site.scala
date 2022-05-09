// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import io.circe._
import edu.gemini.spModel.core.Site

trait SiteCodec {
  import tokens._

  implicit val EncoderSite: Encoder[Site] =
    Encoder[String].contramap(_.abbreviation)

  implicit val DecoderSite: Decoder[Site] =
    Decoder[String].emap(s => Option(Site.parse(s)).toRight(s"Not a valid site: $s"))

  implicit val EncoderListSite: Encoder[List[Site]] =
    encodeTokens(_.abbreviation)

  implicit val DecoderListSite: Decoder[List[Site]] =
    decodeTokens(s => Option(Site.parse(s)).toRight(s"Not a valid site: $s"))

  implicit val KeyEncoderSite: KeyEncoder[Site] =
    KeyEncoder.instance(_.abbreviation)

  implicit val KeyDecoderSite: KeyDecoder[Site] =
    KeyDecoder.instance(s => Option(Site.parse(s)))

}

object site extends SiteCodec