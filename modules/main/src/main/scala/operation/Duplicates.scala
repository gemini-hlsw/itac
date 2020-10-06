// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
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
import edu.gemini.tac.qengine.api.QueueEngine
import java.nio.file.Path
import itac.QueueResult
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.spModel.core.ProgramId

object Duplicates {

  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path],
    tolerance:      Angle
  ): Operation[F] =
    new AbstractQueueOperation[F](qe, siteConfig, rolloverReport) {

      def printTarget(key: TargetDuplicationChecker.ClusterKey, pidMap: Map[String, ProgramId])(member: TargetDuplicationChecker.ClusterMember): Unit = {
        val target = member.target
        val ra     = HourAngle.HMS(Angle.hourAngle.get(Angle.fromDoubleDegrees(target.ra.mag))).format
        val dec    = Angle.DMS(Angle.fromDoubleDegrees(target.dec.mag)).format
        println(f"${key.reference}%-15s  ${pidMap.get(key.reference).fold("--")(_.toString)}%-15s ${key.pi.lastName.take(15)}%-20s  ${member.instrument.take(20)}%-20s  $ra%16s $dec%16s  ${target.name.orEmpty.take(20)}%-20s")
      }

      def checkForDuplicates(proposals: List[Proposal], pidMap: Map[String, ProgramId]): F[Unit] = {
        val tdc  = new TargetDuplicationChecker(proposals, tolerance)
        val dups = tdc.allClusters
        Sync[F].delay {
            println()
            println(s"${Colors.BOLD}Target Duplication Report (tolerance ${Angle.fromStringDMS.reverseGet(tolerance)})${Colors.RESET}")
            println()
            println(s"${Colors.BOLD}Reference        Program ID      PI                    Blueprint              Coordinates                       Name${Colors.RESET}")
                                  //US-2020B-154     GS-2020B-Q-220  Kim                   GMOS-S Longslit B600   20:50:18.088800 317:40:52.896000  Gaia DR2 66770775212
        } *> dups.traverse_{ ds =>
          Sync[F].delay {
            ds.foreach { case (p, nec) =>
              nec.toList.foreach(printTarget(p, pidMap))
            }
            println()
          }
        }
      }

    def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
      computeQueue(ws).flatMap { case (ps, qc) =>

        val pidMap =
          for {
            b <- QueueBand.values
            e <- QueueResult(qc).entries(b)
            p <- e.proposals.toList
          } yield (p.ntac.reference, e.programId)

        checkForDuplicates(ps.filter(_.site == qc.context.site), pidMap.toMap).as(ExitCode.Success)
      }

  }

}