// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import cats.implicits._
import edu.gemini.tac.qengine.p1.{ CloudCover, ImageQuality, SkyBackground, WaterVapor, Target, ObservingConditions }
import edu.gemini.tac.qengine.p2.rollover.RolloverObservation
import edu.gemini.tac.qengine.util.Time
import gsp.math.{ Angle => GAngle, HourAngle }
import io.circe.Decoder
import scala.util.Try
import io.circe.Encoder

/**
 * Encoder a `RolloverObservation` as a whitespace-delimited string with RA/Dec in HMS/DMS. Humans
 * will read this and may edit it so it's nice to have to be columnar.Lines look like this. Abundant
 * space is given to ensure that printed rows line up (remember SUBARU is a partner id).
 *
 * US      GN-2019B-Q-138-40  08:05:47.659920   45:41:58.999200 CC50   IQ70   SBAny  WVAny  192.16633 min
 */
trait RolloverObservationCodec {

  implicit val decoderRolloverObservation: Decoder[RolloverObservation] =
    Decoder[String].emap(fromDelimitedString)

  implicit val encoderRolloverObservation: Encoder[RolloverObservation] =
    Encoder[String].contramap(toDelimitedString)

  private def toDelimitedString(o: RolloverObservation): String = {
    val ra = HourAngle.HMS(HourAngle.fromDoubleHours(o.target.ra.toHr.mag)).format
    val dec = GAngle.DMS(GAngle.fromDoubleDegrees(o.target.dec.toDeg.mag)).format
    f"${o.obsId}%-20s $ra%16s $dec%17s ${o.conditions.cc}%-6s ${o.conditions.iq}%-6s ${o.conditions.sb}%-6s ${o.conditions.wv}%-6s ${o.time.toMinutes.value}%6.1f min"
  }

  private def fromDelimitedString(s: String): Either[String, RolloverObservation] =
    s.split("\\s+") match {
      case Array(oid, ra, dec, cc, iq, sb, wv, mins, "min") =>

        def fail(field: String): Nothing =
          sys.error(s"Error parsing RolloverObservation: invalid $field\n$s")

        Try {
          val oidʹ  = oid
          val raʹ   = HourAngle.fromStringHMS.getOption(ra).getOrElse(fail("RA"))
          val decʹ  = GAngle.fromStringDMS.getOption(dec).getOrElse(fail("DEC"))
          val ccʹ   = CloudCover.values.find(_.toString == cc).getOrElse(fail("CC"))
          val iqʹ   = ImageQuality.values.find(_.toString == iq).getOrElse(fail("IQ"))
          val sbʹ   = SkyBackground.values.find(_.toString == sb).getOrElse(fail("SB"))
          val wvʹ   = WaterVapor.values.find(_.toString == wv).getOrElse(fail("WV"))
          val minsʹ = Time.minutes(mins.toDouble)
          RolloverObservation(oidʹ, Target(raʹ.toDoubleDegrees, decʹ.toDoubleDegrees), ObservingConditions(ccʹ, iqʹ, sbʹ, wvʹ), minsʹ)
        } .toEither.leftMap(_.getMessage)

      case _ => Left(s"Invalid rollover file format. Please re-fetch via `itac -f rollover -s` or `-n`")
    }

}

object rolloverobservatiom extends RolloverObservationCodec