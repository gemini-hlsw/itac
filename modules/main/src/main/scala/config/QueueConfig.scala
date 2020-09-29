// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac
package config

import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import edu.gemini.qengine.skycalc.RaBinSize
import edu.gemini.qengine.skycalc.DecBinSize
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.api.queue.time.PartnerTime
import edu.gemini.tac.qengine.p1.QueueBand.QBand1
import edu.gemini.tac.qengine.p1.QueueBand.QBand2
import edu.gemini.tac.qengine.p1.QueueBand.QBand3
import edu.gemini.tac.qengine.p1.QueueBand.QBand4

// queue configuration
final case class QueueConfig(
  site:       Site,
  overfill:   Map[QueueBand, Percent],
  raBinSize:  RaBinSize,
  decBinSize: DecBinSize,
  hours:      Map[Partner, BandTimes],
) {

  object engine {

    val queueTimes: QueueBand => QueueTime = { b =>

      val pt = PartnerTime.fromFunction { p =>
        hours.get(p) match {
          case Some(BandTimes(b1, b2, b3)) =>
             b match {
               case QBand1 => b1
               case QBand2 => b2
               case QBand3 => b3
               case QBand4 => Time.Zero // we don't really count time in Band 4
             }
          case None    => Time.Zero
        }
      }

      new QueueTime(pt, overfill.getOrElse(b, Percent.Zero))

    }

  }

}

object QueueConfig {
  import itac.codec.all._

  implicit val DecoderQueueBand: Decoder[QueueBand] =
    Decoder[Int].emap { n =>
      QueueBand.values.find(_.number == n).toRight(s"No such queue band: $n")
    }

  implicit val DecoderQueue: Decoder[QueueConfig] = deriveDecoder
}

case class BandTimes(band1: Time, band2: Time, band3: Time)
object BandTimes {

  private def t(s: String): Either[String, Time] =
    Either.catchOnly[NumberFormatException](s.toDouble)
      .leftMap(_.getMessage)
      .map(Time.hours)

  implicit val DecoderBandTimes: Decoder[BandTimes] =
    Decoder[String].emap { s =>
      s.trim.split("\\s+") match {
        case Array(b1, b2, b3) => (t(b1), t(b2), t(b3)).mapN(BandTimes.apply)
        case _ => Left("BandTimes: expected three numbers, like 23.8  23.8  15.9")
      }
    }

}