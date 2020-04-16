// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats.effect.ExitCode
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import itac.Workspace
import itac.Operation
import cats.effect.Sync
import edu.gemini.tac.qengine.p1.Proposal
import cats.effect.Blocker
import itac.ObservationDigest
import gsp.math.Angle
import gsp.math.HourAngle
import edu.gemini.tac.qengine.p1.Observation

object Summarize {

  def apply[F[_]: Sync](reference: String): Operation[F] =
    new Operation[F] {

      def summarize(p: Proposal): F[Unit] =
        Sync[F].delay {

          def printObs(band: String)(o: Observation): Unit = {
            val id     = ObservationDigest.digest(o.p1Observation)
            val ra     = HourAngle.HMS(Angle.hourAngle.get(Angle.fromDoubleDegrees(o.target.ra.mag))).format
            val dec    = Angle.DMS(Angle.fromDoubleDegrees(o.target.dec.mag)).format
            val coords = f"${o.conditions.cc}%-5s ${o.conditions.iq}%-5s ${o.conditions.sb}%-5s ${o.conditions.wv}%-5s "
            val hrs    = o.time.toHours.value
            println(f"$id $band%-4s $hrs%5.1fh $coords $ra%16s $dec%16s ${o.target.name.orEmpty}")
          }

          println()
          println(s"Reference: ${p.id.reference}")
          println(s"Title:     ${p.p1proposal.foldMap(_.title)}")
          println(s"PI:        ${p.piName.orEmpty}")
          println(s"Partner:   ${p.ntac.partner.fullName}")
          println(f"Award:     ${p.time.toHours.value}%1.1f hours")
          println()
          p.obsList.foreach(printObs("B1/2"))
          p.band3Observations.foreach(printObs("B3"))
          println()
        }

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        ws.proposal(reference).flatMap(_.traverse(summarize)).as(ExitCode.Success)

  }

}