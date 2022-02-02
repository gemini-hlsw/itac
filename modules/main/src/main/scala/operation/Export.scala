// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import itac._
import java.io.File
import edu.gemini.spModel.core.ProgramId
import cats._
import cats.effect._
import edu.gemini.tac.qengine.api.QueueEngine
import java.nio.file.Path
import sttp.client._
import javax.xml.bind.{ JAXBContext, Marshaller }
import edu.gemini.model.p1.mutable.ObjectFactory
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import edu.gemini.model.p1.mutable.Proposal
import itac.config.Common
import itac.util.ProgIdHash

object Export {

  private lazy val context: JAXBContext =
    JAXBContext.newInstance((new ObjectFactory).createProposal.getClass)

  private lazy val marshaller: Marshaller =
    context.createMarshaller

  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path],
    odbHost:        String,
    odbPort:        Int,
    progids:        List[ProgramId]
  ): Operation[F] =
    new AbstractExportOperation[F](qe, siteConfig, rolloverReport) {

      def export(p: Proposal, pdfFile: File, pid: ProgramId, cc: Common, pih: ProgIdHash): Unit = {

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

    }

}
