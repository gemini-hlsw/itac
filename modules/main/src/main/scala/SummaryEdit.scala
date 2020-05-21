package itac

import cats.implicits._
import edu.gemini.model.p1.immutable._
import gsp.math.RightAscension
import gsp.math.Declination
import edu.gemini.spModel.core.Coordinates
import edu.gemini.tac.qengine.{ p1 => itac }
import io.circe.Decoder
import io.circe.HCursor
// import gsp.math.Angle

  // Summary edits are applied to a p1.immutable value so we parse into those data types.

case class SummaryEdit(
  reference: String,
  ranking:   Double,
  award:     edu.gemini.model.p1.immutable.TimeAmount,
  obsEdits:  List[SummaryEdit.Obs]
) {

  // I'm too tired to do this with lenses

  private def update(os: List[Observation]): List[Observation] =
    os.map { o =>
      obsEdits.find(_.hash == ObservationDigest.digest(o)).fold(o)(_ update o)
    }

  private def update(sa: SubmissionAccept): SubmissionAccept =
    sa.copy(ranking = ranking, recommended = award)

  private def update(sd: SubmissionDecision): SubmissionDecision =
    sd.copy(decision = sd.decision.map(update))

  private def update(sr: SubmissionResponse): SubmissionResponse =
    sr.copy(decision = sr.decision.map(update))

  private def update(ns: NgoSubmission): NgoSubmission =
    ns.copy(response = ns.response.map(update))

  private def update(es: ExchangeSubmission): ExchangeSubmission =
    es.copy(response = es.response.map(update))

  private def update(ss: SpecialSubmission): SpecialSubmission =
    ss.copy(response = ss.response.map(update))

  private def update(lps: LargeProgramSubmission): LargeProgramSubmission =
    lps.copy(response = lps.response.map(update))

  private def update(sips: SubaruIntensiveProgramSubmission): SubaruIntensiveProgramSubmission =
    sips.copy(response = sips.response.map(update))

  private def update(fts: FastTurnaroundSubmission): FastTurnaroundSubmission =
    fts.copy(response = fts.response.map(update))

  // we assume only one submission has a response
  private def update(e: Either[List[NgoSubmission], ExchangeSubmission]): Either[List[NgoSubmission], ExchangeSubmission] =
    e.bimap(ns => ns.map(update), update)

  private def update(pc: QueueProposalClass): QueueProposalClass =
    pc.copy(subs = update(pc.subs))

  private def update(pc: ClassicalProposalClass): ClassicalProposalClass =
    pc.copy(subs = update(pc.subs))

  private def update(pc: SpecialProposalClass): SpecialProposalClass =
    pc.copy(sub = update(pc.sub))

  private def update(pc: ExchangeProposalClass): ExchangeProposalClass =
    pc.copy(subs = pc.subs.map(update))

  private def update(pc: LargeProgramClass): LargeProgramClass =
    pc.copy(sub = update(pc.sub))

  private def update(pc: SubaruIntensiveProgramClass): SubaruIntensiveProgramClass =
    pc.copy(sub = update(pc.sub))

  private def update(ft: FastTurnaroundProgramClass): FastTurnaroundProgramClass =
    ft.copy(sub = update(ft.sub))

  // N.B. let's not overload here because it will spin if don't implement a case above.
  private def updatePC(pc: ProposalClass): ProposalClass =
    pc match {
      case c: QueueProposalClass => update(c)
      case c: ClassicalProposalClass => update(c)
      case c: SpecialProposalClass => update(c)
      case c: ExchangeProposalClass => update(c)
      case c: LargeProgramClass => update(c)
      case c: SubaruIntensiveProgramClass => update(c)
      case c: FastTurnaroundProgramClass => update(c)
    }

  def update(p: Proposal): Proposal = {
    // TODO: ensure the reference is right!
    p.copy(
      proposalClass = updatePC(p.proposalClass),
      observations  = update(p.observations)
    )
  }

}

object SummaryEdit {

  implicit val DecoderSummaryEdit: Decoder[SummaryEdit] =
    new Decoder[SummaryEdit] {
      def apply(c: HCursor): Decoder.Result[SummaryEdit] =
        for {
          ref   <- c.downField("Reference").as[String]
          rank  <- c.downField("Rank").as[Double]
          award <- c.downField("Award").as[Double].map(TimeAmount(_, TimeUnit.HR))
          obs   <- c.downField("Observations").as[Map[String, List[Obs]]].map(_.values.toList.combineAll)
        } yield SummaryEdit(ref, rank, award, obs)
    }

  // These need to be applied to a p1.immutable value so we parse into those data types.
  case class Obs(
    hash: String,
    band: Band,
    time: TimeAmount,
    cc:   CloudCover,
    iq:   ImageQuality,
    sb:   SkyBackground,
    wv:   WaterVapor,
    ra:   RightAscension,
    dec:  Declination,
    name: String
  ) {

    private def updateConditions(c: Condition): Condition =
      c.copy(cc = cc, iq = iq, wv = wv)

    private def coords: Coordinates =
      Coordinates.fromDegrees(
        ra.toAngle.toDoubleDegrees,
        dec.toAngle.toDoubleDegrees
      ).getOrElse(sys.error(s"Invalid coords: $ra $dec"))

    private def updateSiderealTarget(t: SiderealTarget): SiderealTarget =
      t.copy(name = name, coords = coords)

    private def updateTarget(t: Target): SiderealTarget =
      t match {
        case st: SiderealTarget => updateSiderealTarget(st)
        case _                  => throw new ItacException("Edits to ToO and nonsidereal targets must be done in the PIT.")
      }

    def update(o: Observation): Observation = {
      require(ObservationDigest.digest(o) == hash, "Obs digests don't match.")
      o.copy(
        band      = band,
        progTime  = Some(time),
        condition = o.condition.map(updateConditions),
        target    = o.target.map(updateTarget)
      )
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

    private def timeFromString(s: String): Either[String, TimeAmount] =
      (s.dropRight(1), s.takeRight(1)) match {
        case (s, "h") => Either.catchNonFatal(TimeAmount(s.toDouble, TimeUnit.HR)).leftMap(_ => s"Invalid time: $s")
        case _        => Left(s"Invalid time: $s")
       }

    private def ccFromString(s: String): Either[String, CloudCover] =
      PartialFunction.condOpt(s)(Map(
        itac.CloudCover.CC50.toString  -> CloudCover.BEST,
        itac.CloudCover.CC70.toString  -> CloudCover.CC70,
        itac.CloudCover.CC80.toString  -> CloudCover.CC80,
        itac.CloudCover.CCAny.toString -> CloudCover.ANY,
      )).toRight(s"Invalid CC: $s")

    private def iqFromString(s: String): Either[String, ImageQuality] =
      PartialFunction.condOpt(s)(Map(
        itac.ImageQuality.IQ20.toString  -> ImageQuality.BEST,
        itac.ImageQuality.IQ70.toString  -> ImageQuality.IQ70,
        itac.ImageQuality.IQ85.toString  -> ImageQuality.IQ85,
        itac.ImageQuality.IQAny.toString -> ImageQuality.IQANY,
      )).toRight(s"Invalid IQ: $s")

    private def sbFromString(s: String): Either[String, SkyBackground] =
      PartialFunction.condOpt(s)(Map(
        itac.SkyBackground.SB20.toString  -> SkyBackground.BEST,
        itac.SkyBackground.SBAny.toString -> SkyBackground.ANY,
      )).toRight(s"Invalid SB: $s")

    private def wvFromString(s: String): Either[String, WaterVapor] =
      PartialFunction.condOpt(s)(Map(
        itac.WaterVapor.WV20.toString  -> WaterVapor.BEST,
        itac.WaterVapor.WVAny.toString -> WaterVapor.ANY,
      )).toRight(s"Invalid SB: $s")

    //  bcecb5a8  B1/2    0.3h  CC70  SB70  SBAny WVAny    20:34:13.370299   28:09:50.826099  my name
    def fromString(s: String): Either[String, Obs] =
      s.trim.split("\\s+", 10) match {
        case Array(hash, band, time, cc, iq, sb, wv, ra, dec, name) =>
          for {
            h <- hashFromString(hash)
            b <- bandFromString(band)
            t <- timeFromString(time)
            c <- ccFromString(cc)
            i <- iqFromString(iq)
            s <- sbFromString(sb)
            w <- wvFromString(wv)
            r <- RightAscension.fromStringHMS.getOption(ra).toRight(s"Invalid RA: $ra")
            d <- Declination.fromStringSignedDMS.getOption(dec).toRight(s"Invalid Dec: $dec")
          } yield Obs(h, b, t, c, i, s, w, r, d, name)
        case _ => Left("Not enough fields. Expected hash, band, time, cc, iq, sb, wv, ra, dec, name")
      }

    implicit val DecoderObs: Decoder[Obs] =
      Decoder.decodeString.emap(fromString)

  }

}



