// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import io.chrisdavenport.log4cats.Logger
import cats.effect.ExitCode
import cats.effect.Blocker

trait Operation[F[_]] {
  def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode]
}
