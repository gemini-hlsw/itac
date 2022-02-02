// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac
package operation

import itac.util.Colors
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.api.QueueEngine
import java.nio.file.Path
import io.chrisdavenport.log4cats.Logger
import cats.effect.Blocker
import cats._
import cats.effect._
import cats.implicits._

object Blueprints {

  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path]
  ): Operation[F] =
    new AbstractQueueOperation[F](qe, siteConfig, rolloverReport) {

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        computeQueue(ws).flatMap { case (_, qc) =>
          val qr = QueueResult(qc)
          Sync[F].delay {
            println(s"${Colors.BOLD}Blueprint Report for ${qc.context.site}-${qc.context.semester}.${Colors.RESET}")
            QueueBand.values.foreach { qb =>
              println(s"\n${Colors.BOLD}The following proposals were accepted for Band ${qb.number}${Colors.RESET}\n")
              val color =
              qb.number match {
                case 1 => Colors.YELLOW
                case 2 => Colors.GREEN
                case 3 => Colors.BLUE
                case 4 => Colors.RED
              }
              for {
                e  <- qr.entries(qb)
                p  <- e.proposals.toList
              } {
                println(f"${color}${p.ntac.ranking.num.orEmpty}%5.1f ${p.id.reference}%-15s ${p.piName.orEmpty.take(20)}%-20s ${p.time.toHours.value}%5.1f h  ${e.programId}${Colors.RESET}\n")
                val bps =  p.obsListFor(qb).flatMap(_.p1Observation.blueprint.map(_.name)).distinct
                bps.foreach { bp => println(s"        $bp") }
                println()
              }
            }
            ExitCode.Success
          }

        }

    }

}