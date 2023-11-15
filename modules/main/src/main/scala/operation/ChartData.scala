// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac
package operation

import cats._
import cats.effect._
import cats.implicits._
import edu.gemini.tac.qengine.api.QueueEngine
import edu.gemini.model.p1.immutable.TooTarget
import edu.gemini.model.p1.immutable.VisitorBlueprint
import org.typelevel.log4cats.Logger
import java.nio.file.Path
import itac.util.Colors
import edu.gemini.tac.qengine.p1.QueueBand

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

            def hoursByRAandInstrument(band: QueueBand): Map[(String, Int), Double] =
              qc.queue(band).toList.foldMap { p =>

                val os        = p.obsList ++ p.band3Observations
                val awarded   = p.time.toHours.value
                val estimated = os.foldMap(_.time.toHours.value)
                val ratio     = awarded / estimated

                os.foldMap { o =>
                  val scaledTime = o.time.toHours.value * ratio
                  val hour       = o.p1Observation.target match { case Some(TooTarget(_, _)) => -1; case _ => o.target.ra.toHr.mag.toInt }
                  val instrument = o.p1Observation.blueprint.foldMap {
                    case VisitorBlueprint(_, name) => name.trim
                    case b                         => b.name.takeWhile(_ != ' ')
                   }
                  Map((instrument, hour) -> scaledTime)
                }

              }

            println(s"""|
                        |${Colors.BOLD}Queue Chart Data${Colors.RESET}
                        |
                        |Instrument Time by Band and RA
                        |
                        |For each band, select and copy the following lines, then paste into a Google Sheet. A clipboard icon
                        |will appear, click it and select "Split text to columns", then click the chart icon (on the right side
                        |of the toolbar). Under Chart Type select the stacked column chart. Empty bands are omitted.
                        |""".stripMargin)

            for (b <- QueueBand.values) {
              val map = hoursByRAandInstrument(b)
              if (map.nonEmpty) {
                println(s"\nBand ${b.number}:\n")
                println((-1 to 23).map(d => f"$d%7d").mkString("Hour        ", "", ""))
                for (i <- map.keys.map(_._1).toList.distinct.sorted) {
                  val times = (-1 to 23)
                    .map { h => map.getOrElse((i, h), 0.0) }
                    .map(d => f"$d%7.2f").mkString(i.padTo(12, ' '), "", "")
                  println(times)
                }
              }
            }

            println(s"""|
                        |Instrument Time Totals
                        |
                        |Select and copy the following lines, then paste into a Google Sheet. A clipboard icon will appear,
                        |click it and select "Split text to columns", then click the chart icon (on the right side of the
                        |toolbar). Under Chart Type select the pie chart.
                        |""".stripMargin)

            val all = QueueBand.values.foldMap(hoursByRAandInstrument)
            for (i <- all.keys.map(_._1).toList.distinct.sorted) {
              val time = (-1 to 23).toList.foldMap { h => all.getOrElse((i, h), 0.0) }
              println(f"${i}%-12s $time%7.2f")
            }

            println()
            ExitCode.Success
          }

        }

  }

}


