// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac
package operation

import cats.effect._
import cats.implicits._
import edu.gemini.spModel.core.Site
import edu.gemini.tac.qengine.p2.rollover.RolloverReport
import io.chrisdavenport.log4cats.Logger
import java.net.URL
import scala.xml.Elem
import scala.xml.XML
import cats.Parallel
import java.nio.file.{ Path, Paths }

object Rollover {

  def apply[F[_]: ContextShift: Parallel: Sync](
    site:  Site,
    out:   Option[Path]
  ): Operation[F] =
    new Operation[F] {

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] = {

        val fetchXml: F[Elem] =
          log.info(s"Fetching current rollover report from ${site.displayName}...") *> {
          val host = if (site == Site.GN) "gnodb" else "gsodb"
          val url  = s"http://$host.gemini.edu:8442/rollover"
          b.blockOn(Sync[F].delay(XML.load(new URL(url))))
        }

        val fetchRolloverReport: F[Either[String, RolloverReport]] =
          fetchXml.map(RolloverReport.fromXml)

        fetchRolloverReport.flatMap {
          case Left(msg) => Sync[F].raiseError(ItacException(msg))
          case Right(rr) =>
            for {
              _   <- log.info(s"Got rollover information for ${rr.semester}")
              cc  <- ws.commonConfig
              exp  = cc.semester.prev
              _   <- Sync[F].raiseError(ItacException(s"Expected rollover data for $exp but the current semester is ${rr.semester}.")).whenA(rr.semester != exp)
              outʹ = out.getOrElse(Paths.get(s"${site.abbreviation.toLowerCase}-rollovers.yaml"))
              _   <- ws.writeRolloveReport(outʹ, rr)
            } yield ExitCode.Success
        }

      }

    }


}
