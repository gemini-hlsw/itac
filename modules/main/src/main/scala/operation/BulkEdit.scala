// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats.effect._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import itac._

object BulkEdit {

  def apply[F[_]: Sync]: Operation[F] =
    new Operation[F] {

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        for {
          ps  <- ws.proposals
          f   <- ws.cwd.map(_.resolve("bulk_edit.xls"))
          _   <- BulkEditFile.createOrUpdate(f.toFile, ps)
        } yield ExitCode.Success

  }

}

