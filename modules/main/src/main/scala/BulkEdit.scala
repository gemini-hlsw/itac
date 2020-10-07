// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import edu.gemini.model.p1.mutable._
import cats.effect.Sync
import edu.gemini.spModel.core.ProgramId
import itac.BulkEdit.Accept
import itac.BulkEdit.Reject

final case class BulkEdit(
  ngoEmail:      Option[String],
  staffEmail:    Option[String],
) {
  import BulkEdit.Disposition

  private def update(itac: Itac, disp: Disposition, too: TooOption): Unit =
    disp match {

      case Accept(pid, band, award) =>
        itac.setReject(null)
        val acc = new ItacAccept
        acc.setAward(award)
        acc.setBand(band)
        ngoEmail.foreach(acc.setEmail)     // email is NGO email
        staffEmail.foreach(acc.setContact) // contact is Gemini email
        acc.setProgramId(pid.toString)
        acc.setRollover(band == 1 && (too == null || too == TooOption.NONE))
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

  private def update(pc: QueueProposalClass, disp: Disposition): Unit =
    if (pc != null) {
      update(pc.getExchange())
      Option(pc.getNgo()).foreach(_.forEach(update))
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, pc.getTooOption)

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
      update(pc.getItac, disp, TooOption.RAPID) // HACK: ensure it doesn't get rollover status
    }

  private def update(pc: SpecialProposalClass, disp: Disposition): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, TooOption.NONE)
    }

  private def update(pc: ExchangeProposalClass, disp: Disposition): Unit =
    if (pc != null) {
      Option(pc.getNgo()).foreach(_.forEach(update))
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, TooOption.NONE)
    }

  private def update(pc: LargeProgramClass, disp: Disposition): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, pc.getTooOption)
    }

  private def update(pc: SubaruIntensiveProgramClass, disp: Disposition): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, pc.getTooOption)
    }

  private def update(pc: FastTurnaroundProgramClass, disp: Disposition): Unit =
    if (pc != null) {
      update(pc.getSubmission())
      if (pc.getItac == null) pc.setItac(new Itac)
      update(pc.getItac, disp, pc.getTooOption)
    }

  private def update(pc: ProposalClassChoice, disp: Disposition): Unit =
    if (pc != null) {
      update(pc.getClassical, disp)
      update(pc.getExchange, disp)
      update(pc.getFastTurnaround, disp)
      update(pc.getLarge, disp)
      update(pc.getQueue, disp)
      update(pc.getSip, disp)
      update(pc.getSpecial, disp)
      update(pc.getFastTurnaround, disp)
    }

  def unsafeApplyUpdate(p: Proposal, disp: Disposition): Unit =
    try {
      update(p.getProposalClass(), disp)
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