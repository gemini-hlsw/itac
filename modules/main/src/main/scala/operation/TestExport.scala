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
import javax.xml.bind.{ JAXBContext, Marshaller }
import edu.gemini.model.p1.mutable.ObjectFactory
import edu.gemini.model.p1.mutable.Proposal
import itac.config.Common
import itac.util.ProgIdHash

object TestExport {

  private lazy val context: JAXBContext =
    JAXBContext.newInstance((new ObjectFactory).createProposal.getClass)

  private lazy val marshaller: Marshaller =
    context.createMarshaller

  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path],
    progids:        List[ProgramId]
  ): Operation[F] =
    new AbstractExportOperation[F](qe, siteConfig, rolloverReport) {

      def export(p: Proposal, pdfFile: File, pid: ProgramId, cc: Common, pih: ProgIdHash): Unit = {

        // If we gave an explicit list of progids, make sure pid is in it
        if (progids.nonEmpty && !progids.contains(pid))
          return; //

        println(s"==> exporting <#${System.identityHashCode(p).toHexString}> ${pid} with ${pdfFile.getName} ... ")

        // Serialize the proposal to XML.
        marshaller.marshal(p, System.out)
        System.out.flush()

      }

    }

}
