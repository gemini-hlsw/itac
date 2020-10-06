// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import io.circe._
import edu.gemini.tac.qengine.p1.QueueBand

trait QueueBandCodec {

  implicit val KeyDecoderQueueBand: KeyDecoder[QueueBand] =
    new KeyDecoder[QueueBand] {
      def apply(key: String): Option[QueueBand] =
        QueueBand.values.find(_.productPrefix == key)
    }

}

object queueband extends QueueBandCodec