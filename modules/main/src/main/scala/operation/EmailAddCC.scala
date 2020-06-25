// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import itac._
import java.io.File
import edu.gemini.spModel.core.ProgramId
import cats._
import cats.effect._
import edu.gemini.tac.qengine.api.QueueEngine
import java.nio.file.Path
import edu.gemini.model.p1.mutable._
import itac.config.Common
import edu.gemini.util.security.auth.ProgIdHash
import java.nio.file.Files
import scala.collection.JavaConverters._
import scala.collection.immutable.Nil

// fix for 2020B emails that had no CC
object EmailAddCC {
  import EmailGen.ProposalOps

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

        // CC addrs that we want to add
        val ccAddrs: List[String] =
          List(p.getItacAccept.getEmail, p.getItacAccept.getContact)
            .filterNot(s => s == null || s.trim.isEmpty)

        // We will insert this between the TO and SUBJECT lines.
        val lineToInsert: String =
          s"CC:      ${ccAddrs.mkString(", ")}"

        // The file we're modifying
        val emailFolder = pdfFile.toPath.getParent.getParent.resolve("emails")
        val emailFile   = emailFolder.resolve(s"${p.getItacAccept.getProgramId}.txt")

        // Update the file if it exists.
        if (emailFile.toFile.exists) {
          Files.readAllLines(emailFile).asScala.toList match {
            case Nil => sys.error(s"$emailFile is empty!")
            case h :: t =>
              val newText = (h :: lineToInsert :: t).mkString("\n")
              Files.write(emailFile, newText.getBytes("UTF-8"))
              println(s"UPDATED $pid with $lineToInsert")
          }
        } else {
          println(s"SKIPPING $pid because there is no existing email file.")
        }

      }

    }

}
