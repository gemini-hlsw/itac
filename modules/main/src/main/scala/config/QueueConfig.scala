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

// queue configuration
final case class QueueConfig(
  site:       Site,
  overfill:   Map[QueueBand, Percent],
  raBinSize:  RaBinSize,
  decBinSize: DecBinSize,
  hours:      Map[Partner, BandTimes],
  explicitAssignments: Option[Map[String, QueueBand]] // to allow missing in YAML
) {

  object engine {

    val explicitQueueTime: QueueTime = {

      val categorizedTimes: Map[(Partner, QueueBand), Time] =
        hours.toList.flatMap { case (p, BandTimes(b1, b2, b3)) =>
          List(
            (p, QueueBand.QBand1) -> b1,
            (p, QueueBand.QBand2) -> b2,
            (p, QueueBand.QBand3) -> b3,
          )
        } .toMap

      new QueueTime(categorizedTimes, overfill)

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