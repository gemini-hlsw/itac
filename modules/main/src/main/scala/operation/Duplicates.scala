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
import itac.ObservationDigest
import edu.gemini.tac.qengine.p1.Observation
import gsp.math.HourAngle
import gsp.math.Angle
import edu.gemini.tac.qengine.p1.Proposal

object Duplicates {

  def apply[F[_]: Sync: Parallel](tolerance: Angle): Operation[F] =
    new Operation[F] {

      def printObs(ref: String)(o: Observation): Unit = {
        val id     = ObservationDigest.digest(o.p1Observation)
        val ra     = HourAngle.HMS(Angle.hourAngle.get(Angle.fromDoubleDegrees(o.target.ra.mag))).format
        val dec    = Angle.DMS(Angle.fromDoubleDegrees(o.target.dec.mag)).format
        val coords = f"${o.conditions.cc}%-5s ${o.conditions.iq}%-5s ${o.conditions.sb}%-5s ${o.conditions.wv}%-5s "
        val hrs    = o.time.toHours.value
        println(f"$ref%-20s $id $hrs%5.1fh $coords $ra%16s $dec%16s ${o.target.name.orEmpty}")
      }

      def checkForDuplicates(proposals: List[Proposal]): F[Unit] = {
        val tdc = new TargetDuplicationChecker(proposals, tolerance)
        Sync[F].delay {
            println()
            println(s"${Console.BOLD}Target Duplication Report${Console.RESET}")
            println(s"Grouped targets differ by an angular distance ≤ ${Angle.fromStringDMS.reverseGet(tolerance)}.")
            println()
        } *> tdc.duplicates.traverse_{ ds =>
          Sync[F].delay {
            ds.foreach { case (p, nel) =>
              val obs = (p.obsList ++ p.band3Observations).filter(o => nel.contains_(ObservationDigest.digest(o.p1Observation)))
              obs.foreach(printObs(p.id.reference))
            }
            println()
          }
        }
      }

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] = {
        for {
          ps <- ws.proposals.map(_.sortBy(_.id))
          _  <- checkForDuplicates(ps)
        } yield ExitCode.Success
      }

  }

}