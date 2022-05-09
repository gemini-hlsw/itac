// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import itac._
import java.io.File
import edu.gemini.spModel.core.ProgramId
import cats._
import cats.effect._
import edu.gemini.tac.qengine.api.QueueEngine
import java.nio.file.Path
// import javax.xml.bind.{ JAXBContext, Marshaller }
// import edu.gemini.model.p1.mutable.ObjectFactory
import edu.gemini.model.p1.mutable._
import itac.config.Common
import itac.util.ProgIdHash
import scala.jdk.CollectionConverters._
import edu.gemini.tac.qengine.p1

object TestExport {

  // private lazy val context: JAXBContext =
  //   JAXBContext.newInstance((new ObjectFactory).createProposal.getClass)

  // private lazy val marshaller: Marshaller =
  //   context.createMarshaller

  private def itac(pc: ProposalClassChoice): Option[Itac] =
    Option(pc.getClassical).flatMap(pc => Option(pc.getItac))       orElse
    Option(pc.getExchange).flatMap(pc => Option(pc.getItac))        orElse
    Option(pc.getFastTurnaround).flatMap(pc => Option(pc.getItac))  orElse
    Option(pc.getLarge).flatMap(pc => Option(pc.getItac))           orElse
    Option(pc.getQueue).flatMap(pc => Option(pc.getItac))           orElse
    Option(pc.getSip).flatMap(pc => Option(pc.getItac))             orElse
    Option(pc.getSpecial).flatMap(pc => Option(pc.getItac))

  private def too(pc: ProposalClassChoice): Option[TooOption] =
    Option(pc.getFastTurnaround).flatMap(pc => Option(pc.getTooOption))  orElse
    Option(pc.getLarge).flatMap(pc => Option(pc.getTooOption))           orElse
    Option(pc.getQueue).flatMap(pc => Option(pc.getTooOption))           orElse
    Option(pc.getSip).flatMap(pc => Option(pc.getTooOption))

  private def too(pc: Proposal): Option[TooOption] =
    too(pc.getProposalClass())

  private def itacAccept(pc: Proposal): Option[ItacAccept] =
    itac(pc.getProposalClass()).flatMap(i => Option(i.getAccept))

  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path],
    progids:        List[ProgramId]
  ): Operation[F] =
    new AbstractExportOperation[F](qe, siteConfig, rolloverReport) {

      def checkFile(f: File): String =
        (f.getName + (if (f.exists) "" else " *")).padTo(35, ' ')

      def export(p: Proposal, pdfs: p1.Proposal.Pdfs[File], pid: ProgramId, cc: Common, pih: ProgIdHash): Unit = {

        // If we gave an explicit list of progids, make sure pid is in it
        if (progids.nonEmpty && !progids.contains(pid))
          return; //

        // ITAC results
        val ia: ItacAccept =
          itacAccept(p).getOrElse(sys.error(s"Program $pid has no ItacAccept node."))

        val r = if (ia.isRollover()) "R" else " "

        val i = InstrumentScientistSpreadsheet.Instrument.forBlueprint(p.getObservations().getObservation().asScala.head.getBlueprint())

        val t =
          too(p) match {
            case Some(TooOption.NONE)     => "none"
            case Some(TooOption.RAPID)    => "rapid"
            case Some(TooOption.STANDARD) => "standard"
            case None                     => "--"
          }

        println(f"${System.identityHashCode(p).toHexString}%-10s ${pid}%-17s ${pdfs.map(checkFile).toList.mkString("  ")} ${ia.getBand()} $t%-10s $i%-12s $r")


        // Serialize the proposal to XML.
        // marshaller.marshal(p, System.out)
        // System.out.flush()

      }

    }

}

