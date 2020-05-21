// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.implicits._
import edu.gemini.model.p1.mutable._
import edu.gemini.model.p1.{ immutable => im }
import gsp.math.RightAscension
import gsp.math.Declination
import edu.gemini.tac.qengine.{ p1 => itac }
import io.circe.Decoder
import io.circe.HCursor

/**
 * It turns out we can't really go back from the immutable model to the mutable one. The PIT must
 * do some awful things to make it work. Here we're providing an editor for the initial mutable
 * model we load from disk, before anything else ever sees it.
 */
case class SummaryEdit(
  reference: String,
  ranking:   Double,
  award:     edu.gemini.model.p1.mutable.TimeAmount,
  obsEdits:  List[SummaryEdit.Obs]
) {

  private def update(os: java.util.List[Observation]): Unit = {
    os.forEach { o =>
      obsEdits.find(_.hash == ObservationDigest.digest(im.Observation(o))).foreach(_ update o)
    }
    os.removeIf { o => !o.isEnabled() }
    ()
  }

  private def update(sa: SubmissionAccept): Unit =
    if (sa != null) {
      sa.setRanking(BigDecimal(ranking).bigDecimal)
      sa.setRecommend(award)
    }

  private def update(sr: SubmissionResponse): Unit =
    if (sr != null) {
      update(sr.getAccept())
    }

  private def update(ns: NgoSubmission): Unit =
    if (ns != null) {
      update(ns.getResponse())
    }

  private def update(es: ExchangeSubmission): Unit =
    if (es != null) {
      update(es.getResponse())
    }

  private def update(ss: SpecialSubmission): Unit =
    if (ss != null) {
      update(ss.getResponse())
    }

  private def update(lps: LargeProgramSubmission): Unit =
    if (lps != null) {
      update(lps.getResponse())
    }

  private def update(sips: SubaruIntensiveProgramSubmission): Unit =
    if (sips != null) {
    update(sips.getResponse())
    }

  private def update(fts: FastTurnaroundSubmission): Unit =
    if (fts != null) {
      update(fts.getResponse())
    }

  private def update(pc: QueueProposalClass): Unit =
    if (pc != null) {
      update(pc.getExchange())
      Option(pc.getNgo()).foreach(_.forEach(update))
    }

  private def update(pc: ClassicalProposalClass): Unit =
    if (pc != null) {
      Option(pc.getNgo()).foreach(_.forEach(update))
      update(pc.getExchange())
    }

  private def update(pc: SpecialProposalClass): Unit =
    if (pc != null) {
      update(pc.getSubmission())
    }

  private def update(pc: ExchangeProposalClass): Unit =
    if (pc != null) {
      Option(pc.getNgo()).foreach(_.forEach(update))
    }

  private def update(pc: LargeProgramClass): Unit =
    if (pc != null) {
      update(pc.getSubmission())
    }

  private def update(pc: SubaruIntensiveProgramClass): Unit =
    if (pc != null) {
      update(pc.getSubmission())
    }

  private def update(ft: FastTurnaroundProgramClass): Unit =
    if (ft != null) {
      update(ft.getSubmission())
    }

  // // N.B. let's not overload here because it will spin if don't implement a case above.
  private def updatePC(pc: ProposalClassChoice): Unit =
    if (pc != null) {
      update(pc.getClassical())
      update(pc.getExchange())
      update(pc.getFastTurnaround())
      update(pc.getLarge())
      update(pc.getQueue())
      update(pc.getSip())
      update(pc.getSpecial())
      update(pc.getFastTurnaround())
    }

  def update(p: Proposal): Unit =
    try {
      update(p.getObservations().getObservation())
      updatePC(p.getProposalClass())
    } catch {
      case e: Exception => throw new ItacException(s"$reference: ${e.getMessage}")
    }

}

object SummaryEdit {

  implicit val DecoderSummaryEdit2: Decoder[SummaryEdit] =
    new Decoder[SummaryEdit] {
      def apply(c: HCursor): Decoder.Result[SummaryEdit] =
        for {
          ref   <- c.downField("Reference").as[String]
          rank  <- c.downField("Rank").as[Double]
          award <- c.downField("Award").as[BigDecimal].map { d => val ta = new TimeAmount(); ta.setUnits(TimeUnit.HR); ta.setValue(d.bigDecimal); ta }
          obs   <- c.downField("Observations").as[Map[String, List[Obs]]].map(_.values.toList.combineAll)
        } yield SummaryEdit(ref, rank, award, obs)
    }

  // These need to be applied to a p1.immutable value so we parse into those data types.
  case class Obs(
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

    private def updateCondition(c: Condition): Unit =
      if (c != null) {
        c.setCc(cc)
        c.setIq(iq)
        c.setSb(sb)
        c.setWv(wv)
      }

    private def updateSiderealTarget(t: SiderealTarget): Unit = {
      t.setName(name)
      t.setDegDeg {
        val cs = new DegDegCoordinates
        cs.setRa(BigDecimal(ra.toAngle.toDoubleDegrees).bigDecimal)
        cs.setDec(BigDecimal(dec.toAngle.toDoubleDegrees).bigDecimal)
        cs
      }
    }

    private def updateTarget(t: Target): Unit =
      if (t != null) {
        t match {
          case st: SiderealTarget => updateSiderealTarget(st)
          case _ => () // TODO: log
              // throw new ItacException(s"$hash: Edits to ToO and nonsidereal targets must be done in the PIT.")
        }
      }

    def update(o: Observation): Unit =
      if (o != null) {
        o.setBand(band)
        updateCondition(o.getCondition)
        updateTarget(o.getTarget)
        o.setEnabled(name != "DISABLE")
      }

  }

  object Obs {

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
    def fromString(s: String): Either[String, Obs] =
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
          } yield Obs(h, b, c, i, s, w, r, d, name)
        case _ => Left("Not enough fields. Expected hash, band, time, cc, iq, sb, wv, ra, dec, name")
      }

    implicit val DecoderObs: Decoder[Obs] =
      Decoder.decodeString.emap(fromString)

  }

}


