// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats._
import cats.effect.ExitCode
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import itac.Workspace
import itac.Operation
import cats.effect.Sync
import edu.gemini.tac.qengine.p1.Proposal
import cats.effect.Blocker

object Ls {

  def apply[F[_]: Sync: Parallel]: Operation[F] =
    new Operation[F] {

      def format(p: Proposal): String =
        f"${p.id.reference}%-20s  ${p.piName.orEmpty}%-20s  ${p.ntac.ranking}%4s  ${p.ntac.partner.id}%6s  ${p.ntac.awardedTime.toHours.value}%5.1f h"

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] = {
        for {
          ps <- ws.proposals.map(_.sortBy(_.id))
          _  <- ps.traverse_(p => Sync[F].delay(println(format(p))))
        } yield ExitCode.Success
      }

  }

}