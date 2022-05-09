// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import edu.gemini.model.p1.mutable._
import cats.effect.Sync
import edu.gemini.spModel.core.ProgramId
import itac.BulkEdit.Accept
import itac.BulkEdit.Reject
import scala.jdk.CollectionConverters._

final case class BulkEdit(
  ngoEmail:      Option[String],
  staffEmail:    Option[String],
) {
  import BulkEdit.Disposition

  private def update(itac: Itac, disp: Disposition, too: TooOption, isVisitorOrNonPersistentProposalType: Boolean): Unit =
    disp match {

      case Accept(pid, band, award) =>
        itac.setReject(null)
        val acc = new ItacAccept
        acc.setAward(award)
        acc.setBand(band)
        ngoEmail.foreach(acc.setEmail)     // email is NGO email
        staffEmail.foreach(acc.setContact) // contact is Gemini email
        acc.setProgramId(pid.toString)
        acc.setRollover(band == 1 && (too == null || too == TooOption.NONE) && (!isVisitorOrNonPersistentProposalType))
        itac.setAccept(acc)

      case Reject =>
        itac.setAccept(null)
        itac.setReject(new ItacReject)
    }

  private def update(sa: SubmissionAccept): Unit =
    if (sa != null) {
      ngoEmail.foreach(sa.setEmail)
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

  private def update(pc: QueueProposalClass, disp: Disposition, isVisitorInstrument: Boolean): Unit =
    if (pc != null) {
      update(pc.getExchange())
      Option(pc.getNgo()).foreach(_.forEach(update))
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, pc.getTooOption, isVisitorInstrument)

      // Set the band 3 min requested time (if any) to be the awarded time (if any).
      val band3request = pc.getBand3Request()
      if (band3request != null) {
        disp match {
          case Accept(_, _, award) => band3request.setMinTime(award)
          case _                   => ()
        }
      }

    }

  private def update(pc: ClassicalProposalClass, disp: Disposition): Unit =
    if (pc != null) {
      Option(pc.getNgo()).foreach(_.forEach(update))
      update(pc.getExchange())
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, TooOption.RAPID, false) // HACK: ensure it doesn't get rollover status
    }

  private def update(pc: SpecialProposalClass, disp: Disposition, isVisitorInstrument: Boolean): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, TooOption.NONE, isVisitorInstrument)
    }

  private def update(pc: ExchangeProposalClass, disp: Disposition, isVisitorInstrument: Boolean): Unit =
    if (pc != null) {
      Option(pc.getNgo()).foreach(_.forEach(update))
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, TooOption.NONE, isVisitorInstrument)
    }

  private def update(pc: LargeProgramClass, disp: Disposition): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, pc.getTooOption, true)
    }

  private def update(pc: SubaruIntensiveProgramClass, disp: Disposition, isVisitorInstrument: Boolean): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, pc.getTooOption, isVisitorInstrument)
    }

  private def update(pc: FastTurnaroundProgramClass, disp: Disposition, isVisitorInstrument: Boolean): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, pc.getTooOption, isVisitorInstrument)
    }

  private def update(pc: ProposalClassChoice, disp: Disposition, isVisitorInstrument: Boolean): Unit =
    if (pc != null) {
      update(pc.getClassical, disp)
      update(pc.getExchange, disp, isVisitorInstrument)
      update(pc.getFastTurnaround, disp, isVisitorInstrument)
      update(pc.getLarge, disp)
      update(pc.getQueue, disp, isVisitorInstrument)
      update(pc.getSip, disp, isVisitorInstrument)
      update(pc.getSpecial, disp, isVisitorInstrument)
      update(pc.getFastTurnaround, disp, isVisitorInstrument)
    }

  def unsafeApplyUpdate(p: Proposal, disp: Disposition): Unit =
    try {

      // sigh
      def mutableBand(n: Int) =
        n match {
          case 1 | 2 => Some(Band.BAND_1_2)
          case 3     => Some(Band.BAND_3)
          case _     => None
        }

      // also sigh
      def blueprints: List[BlueprintBase] =
        disp match {
          case Accept(_, band, _) =>
            mutableBand(band) match {
              case Some(p1b) =>
                p.getObservations().getObservation().asScala.toList.filter(_.getBand == p1b).map(_.getBlueprint()).distinct
              case None => Nil // unpossible
            }
          case Reject => Nil // irrelevant
        }

      // oh no
      def isVisitorBlueprint(bp: BlueprintBase): Boolean =
        bp match {
          // Alopeke, Zorro, IGRINS, GRACES, MAROON-X.
          case _: AlopekeBlueprint => true
          case _: ZorroBlueprint   => true
          case _: MaroonXBlueprint => true
          case _: IgrinsBlueprint  => true
          case _: GracesBlueprint  => true
          case _: VisitorBlueprint => true
          case _                   => false
        }

      update(p.getProposalClass(), disp, blueprints.exists(isVisitorBlueprint))

    } catch {
      case e: Exception => throw new ItacException(e.getMessage())
    }


  def applyUpdate[F[_]: Sync](p: Proposal, disp: Disposition): F[Unit] =
    Sync[F].delay(unsafeApplyUpdate(p, disp))

}

object BulkEdit {

  sealed trait Disposition
  case class  Accept(pid: ProgramId, band: Int, award: TimeAmount) extends Disposition
  case object Reject extends Disposition

}