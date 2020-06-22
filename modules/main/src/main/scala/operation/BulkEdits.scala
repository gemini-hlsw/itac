// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats.effect.ExitCode
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
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
          es <- ws.extras
          ns <- ws.extrasNotSubmitted
          rs <- ws.removed
          _  <- ws.bulkEdits(ps ++ es ++ ns ++ rs)
        } yield ExitCode.Success
      }

  }

}