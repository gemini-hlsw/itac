// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac
package operation

import edu.gemini.tac.qengine.p1.QueueBand
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
        computeQueue(ws).flatMap { case (_, qc) =>
          Sync[F].delay {
            println("export!")

            val qr = QueueResult(qc)

            QueueBand.values.foreach { qb =>

              // qr.entries(qb).filter(_.programId.toString() == "GS-2020B-Q-114").foreach { e =>
              qr.entries(qb).foreach { e =>

                println(s"- ${e.programId}")


                Merge.merge(e.proposals.map(_.p1mutableProposal))

              }

            }



            ExitCode.Success
          }
        }

  }

}


