// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.implicits._
import edu.gemini.model.p1.mutable._
import edu.gemini.model.p1.{ immutable => im }
import io.circe.Decoder
import io.circe.HCursor
import io.chrisdavenport.log4cats.Logger
import cats.effect.Sync

/**
 * Apply edits to the MUTABLE p1 `Proposal` as it is loaded from disk, before it is turned into an
 * immutable version. It's very important that we recognize the sharing relationships between
 * observations, conditions, and targets and make the smallest number of changes that we need,
 * without modifying shared objects (conditions and targets) in-place. This is a rather delicate
 * dance if you're not accustomed to programming with mutable values, as I no longer am.
 */
case class SummaryEdit(
  reference:   String,
  ranking:     Double,
  award:       edu.gemini.model.p1.mutable.TimeAmount,
  obsEdits:    List[SummaryObsEdit],
  itacComment: Option[String]
) {

  private def update(os: java.util.List[Observation], p: Proposal): Unit = {
    os.forEach { o =>
      val digest = ObservationDigest.digest(im.Observation(o))
      obsEdits.find(_.hash == digest) match {
        case Some(e) => e.update(o, p, reference)
        case None => println(s"No digest for $o")
      }
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

  private def updateItacComment(pc: ProposalClass): Unit =
    itacComment.foreach { c =>
      if (pc.getItac == null) pc.setItac(new Itac)
      pc.getItac().setComment(c)
    }

  private def update(pc: QueueProposalClass): Unit =
    if (pc != null) {
      update(pc.getExchange())
      Option(pc.getNgo()).foreach(_.forEach(update))
      updateItacComment(pc)
    }

  private def update(pc: ClassicalProposalClass): Unit =
    if (pc != null) {
      Option(pc.getNgo()).foreach(_.forEach(update))
      update(pc.getExchange())
      updateItacComment(pc)
    }

  private def update(pc: SpecialProposalClass): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      updateItacComment(pc)
    }

  private def update(pc: ExchangeProposalClass): Unit =
    if (pc != null) {
      Option(pc.getNgo()).foreach(_.forEach(update))
      updateItacComment(pc)
    }

  private def update(pc: LargeProgramClass): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      updateItacComment(pc)
    }

  private def update(pc: SubaruIntensiveProgramClass): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      updateItacComment(pc)
    }

  private def update(pc: FastTurnaroundProgramClass): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      updateItacComment(pc)
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

  private def update(p: Proposal): ReferenceTime =
    try {
      update(p.getObservations().getObservation(), p)
      updatePC(p.getProposalClass())
      ReferenceTime.Default // TODO!
    } catch {
      case e: Exception => throw new ItacException(s"$reference: ${e.getMessage}")
    }

  // Pure entry point with logging.
  def applyUpdate[F[_]: Sync: Logger](p: Proposal): F[ReferenceTime] =
    Logger[F].debug(s"Pre-edit\n${SummaryDebug.summary(p)}") *>
    Sync[F].delay(update(p)) <*
    Logger[F].debug(s"Post-edit\n${SummaryDebug.summary(p)}")

}

object SummaryEdit {

  implicit val DecoderSummaryEdit2: Decoder[SummaryEdit] =
    new Decoder[SummaryEdit] {
      def apply(c: HCursor): Decoder.Result[SummaryEdit] =
        for {
          ref   <- c.downField("Reference").as[String]
          rank  <- c.downField("Rank").as[Double]
          award <- c.downField("Award").as[BigDecimal].map { d => val ta = new TimeAmount(); ta.setUnits(TimeUnit.HR); ta.setValue(d.bigDecimal); ta }
          obs   <- c.downField("Observations").as[Map[String, List[SummaryObsEdit]]].map(_.values.toList.combineAll)
          itac  <- c.downField("Comment").as[Option[String]]
        } yield SummaryEdit(ref, rank, award, obs, itac)
    }

}



