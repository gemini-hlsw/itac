// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats._
import cats.effect.ExitCode
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import itac.Workspace
import itac.Operation
import itac.util.TargetDuplicationChecker
import cats.effect.Sync
import cats.effect.Blocker
import gsp.math.HourAngle
import gsp.math.Angle
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.p1.Target

object Duplicates {

  def apply[F[_]: Sync: Parallel](tolerance: Angle): Operation[F] =
    new Operation[F] {

      def printTarget(ref: String)(target: Target): Unit = {
        val ra     = HourAngle.HMS(Angle.hourAngle.get(Angle.fromDoubleDegrees(target.ra.mag))).format
        val dec    = Angle.DMS(Angle.fromDoubleDegrees(target.dec.mag)).format
        println(f"$ref%-20s $ra%16s $dec%16s ${target.name.orEmpty}")
      }

      def checkForDuplicates(proposals: List[Proposal]): F[Unit] = {
        val tdc  = new TargetDuplicationChecker(proposals, tolerance)
        val dups = tdc.allClusters
        Sync[F].delay {
            println()
            println(s"${Console.BOLD}Target Duplication Report${Console.RESET}")
            println(s"Target clusters with minimal spanning tree edges ≤ ${Angle.fromStringDMS.reverseGet(tolerance)}.")
            println()
        } *> dups.traverse_{ ds =>
          Sync[F].delay {
            ds.foreach { case (p, nec) =>
              nec.toList.foreach(printTarget(p))
            }
            println()
          }
        }
      }

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] = {
        for {
          ps <- ws.proposals
          _  <- checkForDuplicates(ps)
        } yield ExitCode.Success
      }

  }

}