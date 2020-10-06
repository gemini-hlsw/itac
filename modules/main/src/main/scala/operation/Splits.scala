// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats.effect.ExitCode
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import itac.Workspace
import itac.Operation
import cats.effect.Sync
import edu.gemini.tac.qengine.p1.Proposal
import cats.effect.Blocker
import itac.util.Colors

object Splits {

  def apply[F[_]: Sync]: Operation[F] =
    new Operation[F] {

      def header: String =
        f"${Colors.BOLD} ${"Id"}%-18s  Site    ${"PI"}%-20s  ${"Rank"}%4s ${"Partner"}%6s   ${"Time"}%6s${Colors.RESET}"

      def format(p: Proposal): String =
        f"${p.id.reference}%-20s  ${p.site.abbreviation}    ${p.piName.orEmpty}%-20s  ${p.ntac.ranking}%4s  ${p.ntac.partner.id}%6s  ${p.ntac.awardedTime.toHours.value}%5.1f h"

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] = {
        for {
          gs <- ws.proposals.map { ps =>
                  ps.groupBy(p => (p.site, p.piName.orEmpty, p.p1proposal.title))
                    .values
                    .filter(_.size > 1)
                    .toList
                    .sortBy { ps => (ps.head.site, ps.head.piName) }
                }
          _  <- Sync[F].delay(println(header))
          _  <- gs.zipWithIndex.traverse_ { case (ps, n) =>
                  Sync[F].delay {
                    if (n % 2 == 0) print(Colors.BLUE)
                    Queue.printWithGroupBars(ps.map(format))
                    print(Colors.RESET)
                  }
                }
        } yield ExitCode.Success
      }

  }

}