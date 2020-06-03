// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

package operation

import edu.gemini.model.p1.mutable.TimeUnit
import edu.gemini.model.p1.mutable.TimeAmount
import edu.gemini.tac.qengine.p1.QueueBand
import java.io.File
import cats._
import cats.effect._
import cats.implicits._
import edu.gemini.tac.qengine.api.QueueEngine
import io.chrisdavenport.log4cats.Logger
import java.nio.file.Path

object Export {

  /**
    * @param siteConfig path to site-specific configuration file, which can be absolute or relative
    *   (in which case it will be resolved relative to the workspace directory).
    */
  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path]
  ): Operation[F] =
    new AbstractQueueOperation[F](qe, siteConfig, rolloverReport) {

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        BulkEditFile.read(new File("/tmp/test.xls")).flatMap { bes =>
        computeQueue(ws).flatMap { case (_, qc) =>
          Sync[F].delay {
            val qr = QueueResult(qc)
            QueueBand.values.foreach { qb =>

              qr.entries(qb).foreach { e =>

                // Before merging we need to apply bulk updates. When we merge we will get a random
                // ITAC node ... which one do we pick? We need to merge them together and add their
                // times and pick a primary NGO!
                e.proposals.toList.foreach { p =>
                  bes.get(p.ntac.reference) match {
                    case Some(be) => be.unsafeApplyUpdate(p.p1mutableProposal, itac.BulkEdit.Accept(e.programId, qb.number, {
                      val ta = new TimeAmount
                      ta.setUnits(TimeUnit.HR)
                      ta.setValue(new java.math.BigDecimal(p.time.toHours.value))
                      ta
                    }))
                    case None => sys.error(s"Proposal ${p.ntac.reference} is not present in the bulk edits file. Cannot create ITAC node.")
                  }
                }

                val p = Merge.merge(e.proposals.map(_.p1mutableProposal))

                println(s"${SummaryDebug.summary(p)}\n-----------------------\n")


              }

              // TODO: also export classicals! they also need an ITAC node
              // Also need rejects!


            }



            ExitCode.Success
          }
        }
      }

  }

}


