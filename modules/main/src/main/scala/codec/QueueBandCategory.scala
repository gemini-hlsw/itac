// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import io.circe._
import edu.gemini.tac.qengine.p1.QueueBand

trait QueueBandCategoryCodec {

  implicit val KeyDecoderQueueBandCategory: KeyDecoder[QueueBand.Category] =
    new KeyDecoder[QueueBand.Category] {
      def apply(key: String): Option[QueueBand.Category] =
        QueueBand.Category.values.find(_.productPrefix == key)
    }

}

object queuebandcategory extends QueueBandCategoryCodec