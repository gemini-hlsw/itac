// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.implicits._
import edu.gemini.model.p1.mutable._
import gsp.math.RightAscension
import gsp.math.Declination
import edu.gemini.tac.qengine.{ p1 => itac }
import io.circe.Decoder
import scala.jdk.CollectionConverters._
import edu.gemini.spModel.core

/**
 * Used by `SummaryEdit` to deal with observations, which is where most of the complications lie.
 */
case class SummaryObsEdit(
  hash: String,
  band: Band,
  cc:   CloudCover,
  iq:   ImageQuality,
  sb:   SkyBackground,
  wv:   WaterVapor,
  ra:   RightAscension,
  dec:  Declination,
  name: String
) {

  /*
   * In order to do this safely we can't ever change an existing `Condition` value because they are
   * shared with other observations. So the strategy is, if the edit we wish to apply would result
   * in another `Condition` that's identical to an existing one, just switch to that to avoid
   * duplication (otherwise changing every SB80 to SB50 would result in N new `Condition`s instead
   * of one). If no such reuse is possible, construct a new one based on the existing one, with
   * changes as specified, add it to the list of conditions, and switch to it.
   */
  def updateCondition(o: Observation, p: Proposal): Unit = {

    // val c = o.getCondition()
    val allConditions = p.getConditions().getCondition().asScala.toList

    val existing: Option[Condition] =
      allConditions.find { c2 =>
        c2.getCc == cc &&
        c2.getIq == iq &&
        c2.getSb == sb &&
        c2.getWv == wv
        // ignore airmass, PI needs to double-check if there have been edits
      }

    val replaceWith: Condition =
      existing match {
        case Some(c2) =>
          // if (c.getId() == c2.getId()) println(f"${System.identityHashCode(o).toHexString}%8s: keeping ${c.getId()}.")
          // else println(f"${System.identityHashCode(o).toHexString}%8s: switching from ${c.getId()} to ${c2.getId()}")
          c2
        case None     =>
          val c2 = new Condition
          c2.setCc(cc)
          c2.setIq(iq)
          c2.setSb(sb)
          c2.setWv(wv)
          // not setting airmass
          c2.setId(s"condition-${allConditions.map(_.getId.dropWhile(!_.isDigit).toInt).max + 1}")
          p.getConditions().getCondition().add(c2) // important!
          // println(f"${System.identityHashCode(o).toHexString}%8s: switching from ${c.getId()} to NEW ${c2.getId()}")
          c2
      }

    o.setCondition(replaceWith)

  }

  /*
   * The logic here is the same as in updateConditions above. We never change a value in-place. If
   * the target is nonsidereal we return its ID and the new reference coordinates.
   */
  def updateTarget(o: Observation, p: Proposal, ref: String): Option[(String, core.Coordinates)] = {

    val allTargets = p.getTargets().getSiderealOrNonsiderealOrToo().asScala

    // are these angular values within 0.0001 of a degree?
    implicit class DoubleOps(a: Double) {
      def ~=(b: Double): Boolean =
        (a - b).doubleValue.abs <= 0.0001
    }

    val existing: Option[Target] =
      allTargets.find {
        case t2: SiderealTarget =>

          // We match a SiderealTarget if it has[very] similar coordinates and the same name.
          (t2.getDegDeg.getRa.doubleValue  ~= ra.toAngle.toDoubleDegrees)        &&
          (t2.getDegDeg.getDec.doubleValue ~= dec.toAngle.toSignedDoubleDegrees) &&
          (t2.getName                      == name)

        case t2: TooTarget =>

          // We match a TooTarget if it has zero coordinates and the same name.
          (t2.getName == name)                &&
          (ra         == RightAscension.Zero) &&
          (dec        == Declination.Zero)

        case _: NonSiderealTarget =>

          // We never match a NonSiderealTarget. Always replace with a sidereal target if it is
          // mentioned in the edits file.
          false

      }

    val replaceWith: Target =
      existing match {
        case Some(t2) =>
          // if (t.getId() == t2.getId()) println(f"${System.identityHashCode(o).toHexString}%8s: keeping ${t.getId()} ${t.getName()}.")
          // else println(f"${System.identityHashCode(o).toHexString}%8s: switching from ${t.getId()} ${t.getName()} to ${t2.getId()} ${t2.getName()}")
          t2
        case None =>

          // Warn in the case where we're clobbering something that's not a SiderealTarget
          val t2: Target =
            o.getTarget match {
              case _: SiderealTarget =>
                val t2 = new SiderealTarget()
                t2.setDegDeg {
                  val dd = new DegDegCoordinates
                  dd.setRa(BigDecimal(ra.toAngle.toDoubleDegrees).bigDecimal)
                  dd.setDec(BigDecimal(dec.toAngle.toSignedDoubleDegrees).bigDecimal)
                  dd
                }
                t2

              case _: TooTarget =>
                new TooTarget()

              case t: NonSiderealTarget =>
                val t2 = new NonSiderealTarget()
                t2.setEpoch(t.getEpoch())
                t2.getEphemeris().addAll(t.getEphemeris)
                t2

          }

          t2.setName(name)
          t2.setId(s"target-${allTargets.map(_.getId.dropWhile(!_.isDigit).toInt).max + 1}")
          // no magnitudes or proper motion, we have no idea
          p.getTargets().getSiderealOrNonsiderealOrToo().add(t2) // important!
          t2
      }

    o.setTarget(replaceWith)

    replaceWith match {
      case  _: NonSiderealTarget =>
        val coords = core.Coordinates.fromDegrees(ra.toAngle.toDoubleDegrees, dec.toAngle.toSignedDoubleDegrees).getOrElse(sys.error(s"Invalid coordinates specified for $ref"))
        Some(replaceWith.getId() -> coords)
      case _ =>
        None
    }

  }

  // Appy this edit to the specified observation, returning a possibly empty mapping from
  // nonsidereal target ids to reference coordinates.
  def update(o: Observation, p: Proposal, ref: String): Option[(String, core.Coordinates)] =
    Option(o).flatMap { o =>
      if (name == "DISABLE") {
        o.setEnabled(false)
        None
      } else {
        o.setBand(band)
        updateCondition(o, p)
        updateTarget(o, p, ref)
      }
    }

}

object SummaryObsEdit {

  private def hashFromString(s: String): Either[String, String] =
      Either.catchNonFatal(BigInt(s, 16)).leftMap(_ => s"Invalid hash: $s").as(s)

  private def bandFromString(s: String): Either[String, Band] =
    s match {
      case "B1/2" => Right(Band.BAND_1_2)
      case "B3"   => Right(Band.BAND_3)
      case _      => Left(s"Invalid band: $s")
    }

  private def ccFromString(s: String): Either[String, CloudCover] =
    PartialFunction.condOpt(s)(Map(
      itac.CloudCover.CC50.toString  -> CloudCover.cc50,
      itac.CloudCover.CC70.toString  -> CloudCover.cc70,
      itac.CloudCover.CC80.toString  -> CloudCover.cc80,
      itac.CloudCover.CCAny.toString -> CloudCover.cc100,
    )).toRight(s"Invalid CC: $s")

  private def iqFromString(s: String): Either[String, ImageQuality] =
    PartialFunction.condOpt(s)(Map(
      itac.ImageQuality.IQ20.toString  -> ImageQuality.iq20,
      itac.ImageQuality.IQ70.toString  -> ImageQuality.iq70,
      itac.ImageQuality.IQ85.toString  -> ImageQuality.iq85,
      itac.ImageQuality.IQAny.toString -> ImageQuality.iq100,
    )).toRight(s"Invalid IQ: $s")

  private def sbFromString(s: String): Either[String, SkyBackground] =
    PartialFunction.condOpt(s)(Map(
      itac.SkyBackground.SB20.toString  -> SkyBackground.sb20,
      itac.SkyBackground.SB50.toString  -> SkyBackground.sb50,
      itac.SkyBackground.SB80.toString  -> SkyBackground.sb80,
      itac.SkyBackground.SBAny.toString -> SkyBackground.sb100,
    )).toRight(s"Invalid SB: $s")

  private def wvFromString(s: String): Either[String, WaterVapor] =
    PartialFunction.condOpt(s)(Map(
      itac.WaterVapor.WV20.toString  -> WaterVapor.wv20,
      itac.WaterVapor.WV50.toString  -> WaterVapor.wv50,
      itac.WaterVapor.WV80.toString  -> WaterVapor.wv80,
      itac.WaterVapor.WVAny.toString -> WaterVapor.wv100,
    )).toRight(s"Invalid SB: $s")

  //  bcecb5a8  B1/2    0.3h  CC70  SB70  SBAny WVAny    20:34:13.370299   28:09:50.826099  my name
  def fromString(s: String): Either[String, SummaryObsEdit] =
    s.trim.split("\\s+", 10) match {
      case Array(hash, band, _, cc, iq, sb, wv, ra, dec, name) =>
        for {
          h <- hashFromString(hash)
          b <- bandFromString(band)
          c <- ccFromString(cc)
          i <- iqFromString(iq)
          s <- sbFromString(sb)
          w <- wvFromString(wv)
          r <- RightAscension.fromStringHMS.getOption(ra).toRight(s"Invalid RA: $ra")
          d <- Declination.fromStringSignedDMS.getOption(dec).toRight(s"Invalid Dec: $dec")
        } yield SummaryObsEdit(h, b, c, i, s, w, r, d, name)
      case _ => Left("Not enough fields. Expected hash, band, time, cc, iq, sb, wv, ra, dec, name")
    }

  implicit val DecoderObs: Decoder[SummaryObsEdit] =
    Decoder.decodeString.emap(fromString)

}



