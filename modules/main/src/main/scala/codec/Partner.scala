// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import io.circe._
import edu.gemini.tac.qengine.ctx.Partner

trait PartnerCodec {

  implicit val EncoderPartner: Encoder[Partner] =
    Encoder[String].contramap(_.id)

  implicit val DecoderPartner: Decoder[Partner] =
    Decoder[String].emap { s =>
      Partner.fromString(s).toRight(s"No such partner: $s")
    }

  implicit val KeyDecoderQueuePartner: KeyDecoder[Partner] =
    new KeyDecoder[Partner] {
      def apply(key: String): Option[Partner] =
        Partner.fromString(key)
    }

}

object partner extends PartnerCodec