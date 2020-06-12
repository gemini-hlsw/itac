// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac
package operation

import cats._
import cats.effect._
import cats.implicits._
import edu.gemini.tac.qengine.api.QueueEngine
import io.chrisdavenport.log4cats.Logger
import java.nio.file.Path
import itac.util.Colors

object ChartData {

  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path]
  ): Operation[F] =
    new AbstractQueueOperation[F](qe, siteConfig, rolloverReport) {

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        computeQueue(ws).flatMap { case (_, qc) =>
          Sync[F].delay {
            val ps = qc.queue.toList

            val hoursByRAandInstrument: Map[(String, Int), Double] =
              ps.foldMap { p =>

                val os        = p.obsList ++ p.band3Observations
                val awarded   = p.time.toHours.value
                val estimated = os.foldMap(_.time.toHours.value)
                val ratio     = awarded / estimated

                os.foldMap { o =>
                  val scaledTime = o.time.toHours.value * ratio
                  val hour       = o.target.ra.toHr.mag.toInt
                  val instrument = o.p1Observation.blueprint.foldMap(_.name.takeWhile(_ != ' ')) // muhahaha

                  Map((instrument, hour) -> scaledTime)
                }

              }

            println(s"""|
                        |${Colors.BOLD}Queue Chart Data${Colors.RESET}
                        |
                        |Instrument Time by RA
                        |
                        |Select and copy the following lines, then paste into a Google Sheet. A clipboard icon will appear,
                        |click it and select "Split text to columns", then click the chart icon (on the right side of the
                        |toolbar). Under Chart Type select the stacked column chart.
                        |""".stripMargin)

            println((0 to 23)
                .map(d => f"$d%7d").mkString("Hour        ", "", ""))

            for (i <- hoursByRAandInstrument.keys.map(_._1).toList.distinct.sorted) {
              val times = (0 to 23)
                .map { h => hoursByRAandInstrument.getOrElse((i, h), 0.0) }
                .map(d => f"$d%7.2f").mkString(i.padTo(12, ' '), "", "")
              println(times)
            }

            println(s"""|
                        |Instrument Time Totals
                        |
                        |Select and copy the following lines, then paste into a Google Sheet. A clipboard icon will appear,
                        |click it and select "Split text to columns", then click the chart icon (on the right side of the
                        |toolbar). Under Chart Type select the pie chart.
                        |""".stripMargin)

            for (i <- hoursByRAandInstrument.keys.map(_._1).toList.distinct.sorted) {
              val time = (0 to 23).toList.foldMap { h => hoursByRAandInstrument.getOrElse((i, h), 0.0) }
              println(f"${i}%-12s $time%7.2f")
            }

            println()
            ExitCode.Success
          }

        }

  }

}


