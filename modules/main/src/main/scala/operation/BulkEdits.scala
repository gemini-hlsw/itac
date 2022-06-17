// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats.effect.ExitCode
import cats.implicits._
import org.typelevel.log4cats.Logger
import itac.Workspace
import itac.Operation
import cats.effect.Sync
import cats.effect.Blocker

object BulkEdits {

  def apply[F[_]: Sync]: Operation[F] =
    new Operation[F] {
      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] = {
        for {
          ps <- ws.proposals
          rs <- ws.removed
          _  <- ws.bulkEdits(ps ++ rs)
        } yield ExitCode.Success
      }

  }

}