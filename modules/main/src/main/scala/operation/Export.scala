// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import itac._
import java.io.File
import edu.gemini.spModel.core.ProgramId
import edu.gemini.tac.qengine.p1.Mode
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.model.p1.mutable.TimeUnit
import edu.gemini.model.p1.mutable.TimeAmount
import edu.gemini.tac.qengine.p1.QueueBand
import cats._
import cats.effect._
import cats.implicits._
import edu.gemini.tac.qengine.api.QueueEngine
import io.chrisdavenport.log4cats.Logger
import java.nio.file.Path
import sttp.client._
import javax.xml.bind.{ JAXBContext, Marshaller }
import edu.gemini.model.p1.mutable.ObjectFactory
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import itac.PrimaryNgo.Info
import cats.data.NonEmptyList

object Export {

  private lazy val context: JAXBContext =
    JAXBContext.newInstance((new ObjectFactory).createProposal.getClass)

  private lazy val marshaller: Marshaller =
    context.createMarshaller

  /**
    * @param siteConfig path to site-specific configuration file, which can be absolute or relative
    *   (in which case it will be resolved relative to the workspace directory).
    */
  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path],
    odbHost:        String,
    odbPort:        Int,
    progids:        List[ProgramId]
  ): Operation[F] =
    new AbstractQueueOperation[F](qe, siteConfig, rolloverReport) {

      def doExport(ps: List[Proposal], qr: QueueResult, bes: Map[String, BulkEdit], cwd: Path): F[ExitCode] =
        Sync[F].delay {

          def addItacNode(p: Proposal, pid: ProgramId, band: QueueBand): Unit =
            bes.get(p.ntac.reference) match {
              case Some(be) => be.unsafeApplyUpdate(p.p1mutableProposal, itac.BulkEdit.Accept(pid, band.number, {
                val ta = new TimeAmount
                ta.setUnits(TimeUnit.HR)
                ta.setValue(new java.math.BigDecimal(p.time.toHours.value))
                ta
              }))
              case None =>
                // This should never happen because we update the bulk-edit file every run
                sys.error(s"Proposal ${p.ntac.reference} is not present in the bulk edits file. Cannot create ITAC node.")
            }

          def export(p: edu.gemini.model.p1.mutable.Proposal, pdfFile: File, pid: ProgramId): Unit = {

            // If we gave an explicit list of progids, make sure pid is in it
            if (progids.nonEmpty && !progids.contains(pid))
              return; //

            println(s"==> exporting <#${System.identityHashCode(p).toHexString}> ${pid} with ${pdfFile.getName} ... ")

            // Serialize the proposal to XML.
            val baos = new ByteArrayOutputStream
            marshaller.marshal(p, baos)
            baos.flush()
            val xmlStream = new ByteArrayInputStream(baos.toByteArray)

            // Skip if no PDF file
            if (!pdfFile.exists()) {
              println(s"${Console.RED}    NO PDF FILE!${Console.RESET}")
              return
            }

            // ok need an http client here that can do multipart
            implicit val backend = HttpURLConnectionBackend()
            val req = basicRequest.multipartBody(
              multipart("proposal", xmlStream).contentType("text/xml"),
              multipartFile("attachment", pdfFile).contentType("application/pdf")
            ).get(uri"http://$odbHost:$odbPort/skeleton?convert=true")

            val res = req.send()

            println(res.code.code)
            if (res.code.code >= 300)
              println(res.body)

          }

          QueueBand.values.foreach { qb =>

              // Non-Queue Proposals ...
              val nonQueue =
                ps.filter(_.site == qr.queueCalc.context.site)                // are at this site
                  .filterNot(p => qr.queueCalc.proposalLog.proposalIds(p.id)) // but don't appear in the log

              // Pick out classicals and those that shouldn't be here
              val (classical, orphans) = nonQueue.partition(_.mode == Mode.Classical)

              // These *should* all be classical. Let's be sure though.
              if (orphans.nonEmpty) {
                orphans.foreach { p =>
                  println(s"${p.ntac.reference} is ${p.mode} and is not in the queue!")
                }
                throw new ItacException("Non-classical program somehow escaped the queue!")
              }

              def pdfFile(name: String): File =
                cwd.resolve(s"pdfs/$name").toAbsolutePath.toFile

              // Get classical *entries* and then add them.
              qr.classical(classical).foreach { e =>
                val p = e.proposals.head // don't handle joints yet, may not matter
                addItacNode(p, e.programId, QueueBand.QBand1)
                export(p.p1mutableProposal, pdfFile(p.p1pdfFile), e.programId)
              }

              // Queue Proposals
              qr.entries(qb).foreach { e =>

                // Add ITAC Accept nodes to each proposal in this queue entry.
                e.proposals.toList.foreach(addItacNode(_, e.programId, qb))

                // Move the one with the primary NGO to the head. We merge everything into it.
                val parts = PrimaryNgo.find(e.proposals.head.p1mutableProposal) match {
                  case None => e.proposals // hope for the best
                  case Some(Info(p, _)) =>
                    val (a, b) = e.proposals.toList.partition(_.ntac.partner.id == p.name)
                    // println(s"primary ngo is $p, part ngos are ${e.proposals.toList.map(_.ntac.partner.id)} ... ${a.length} + ${b.length}")
                    NonEmptyList.fromList(a ++ b).getOrElse(sys.error("unpossible!"))
                }

                 // Merge joints.
                val p = Merge.merge(parts.map(_.p1mutableProposal))

                // debug print the proposal
                // println("─" * 100)
                // println(s"[An] input file is ${e.proposals.head.p1xmlFile.getName} and the PDF file is ${e.proposals.head.p1pdfFile.getName}.")
                // println(SummaryDebug.summary(p))

                export(p, pdfFile(e.proposals.head.p1pdfFile), e.programId)

              }

            }

            // Workbooks for NGOs

            ExitCode.Success
          }

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        for {
          p   <- computeQueue(ws); (ps, qc) = p
          be  <- ws.bulkEdits(ps)
          cwd <- ws.cwd
          e   <- doExport(ps, QueueResult(qc), be, cwd)
        } yield e

  }

}


