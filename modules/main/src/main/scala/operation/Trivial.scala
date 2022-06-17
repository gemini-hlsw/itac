// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac
package operation

import org.typelevel.log4cats.Logger
import cats.effect.Blocker
import cats._
import cats.effect._
import cats.implicits._

object Trivial {

  def apply[F[_]: Applicative]: Operation[F] =
    new Operation[F] {

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        ExitCode.Success.pure[F]

  }

}