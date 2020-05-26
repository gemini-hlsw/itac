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
import itac.util.Colors

object Duplicates {

  def apply[F[_]: Sync: Parallel](tolerance: Angle): Operation[F] =
    new Operation[F] {

      def printTarget(key: TargetDuplicationChecker.ClusterKey)(member: TargetDuplicationChecker.ClusterMember): Unit = {
        val target = member.target
        val ra     = HourAngle.HMS(Angle.hourAngle.get(Angle.fromDoubleDegrees(target.ra.mag))).format
        val dec    = Angle.DMS(Angle.fromDoubleDegrees(target.dec.mag)).format
        println(f"${key.reference}%-15s  ${key.pi.lastName.take(15)}%-20s  ${member.instrument.take(20)}%-20s  $ra%16s $dec%16s  ${target.name.orEmpty.take(20)}%-20s")
      }

      def checkForDuplicates(proposals: List[Proposal]): F[Unit] = {
        val tdc  = new TargetDuplicationChecker(proposals, tolerance)
        val dups = tdc.allClusters
        Sync[F].delay {
            println()
            println(s"${Colors.BOLD}Target Duplication Report (tolerance ${Angle.fromStringDMS.reverseGet(tolerance)})${Colors.RESET}")
            println()
            println(s"${Colors.BOLD}Reference        PI                    Blueprint              Coordinates                       Name${Colors.RESET}")
                                  //CL-2020B-006     Kalari                Zorro Speckle (0.009   05:37:51.009999 290:50:26.100000  LMC_J053751.00-69093
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