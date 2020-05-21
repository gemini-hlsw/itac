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
import cats.Order
import edu.gemini.tac.qengine.p1.Observation
import cats.data.NonEmptyList
import itac.Summary
import itac.util.OneOrTwo
import itac.Summary.BandedObservation
import java.nio.file.Paths
import edu.gemini.tac.qengine.p1.CloudCover
import edu.gemini.tac.qengine.p1.ImageQuality
import edu.gemini.tac.qengine.p1.SkyBackground
import edu.gemini.tac.qengine.p1.WaterVapor

object Summarize {

  private implicit class ObservationOps(o: Observation) {
    def ra:  HourAngle = Angle.hourAngle.get(Angle.fromDoubleDegrees(o.target.ra.mag))
    def dec: Angle     = Angle.fromDoubleDegrees(o.target.dec.mag)
  }

  final case class Field(name: String, order: Order[BandedObservation])
  object Field {

    val band  = Field("band",  Order.by(o => o.band))
    val hash  = Field("hash",  Order.by(o => ObservationDigest.digest(o.obs.p1Observation)))
    val ra    = Field("ra",    Order.by(o => o.obs.ra.toDoubleDegrees))
    val dec   = Field("dec",   Order.by(o => o.obs.dec.toSignedDoubleDegrees))
    val award = Field("award", Order.by(o => o.obs.time.toHours.value))
    val name  = Field("name",  Order.by(o => o.obs.target.name.orEmpty.toLowerCase()))

    val all: List[Field] =
      List(band, hash, ra, dec, award, name)

    def fromString(name: String): Either[String, Field] =
      all.find(_.name.toLowerCase == name)
         .toRight(s"No such field: $name. Try one or more of ${all.map(_.name).mkString(",")}")

    def parse(ss: String): Either[String, NonEmptyList[Field]] =
      ss.split(",")
        .toList
        .traverse(fromString)
        .flatMap { fs =>
          NonEmptyList
            .fromList(fs)
            .toRight(s"No fields specified. Try one or more of ${all.map(_.name).mkString(",")}")
        }

  }

  def apply[F[_]: Sync](reference: String, fields: NonEmptyList[Field], edit: Boolean): Operation[F] =
    new Operation[F] {

      def summarize(ws: Workspace[F], ps: NonEmptyList[Proposal]): F[Unit] = {

        val summary: Summary =
          OneOrTwo.fromFoldable(ps) match {
            case Some(ot) => Summary(ot)
            case None => sys.error("wat? there were more than two slices??!?")
          }

        implicit val ordering = fields.reduceMap(_.order)(Order.whenEqualMonoid).toOrdering

        val header =
          s"""|# Edit file for ${summary.reference}
              |# You may edit [only] the following fields/columns.
              |# - Award as Decimal Hours
              |# - Rank  as Decimal
              |# - Band  as B1/2, B3
              |# - CC    as ${CloudCover.values.mkString(", ")}
              |# - IQ    as ${ImageQuality.values.mkString(", ")}
              |# - SB    as ${SkyBackground.values.mkString(", ")}
              |# - WV    as ${WaterVapor.values.mkString(", ")}
              |# - RA    as HMS
              |# - Dec   as Signed DMS(signed dms)
              |# - Name  as Text, set to DISABLE to disable observation
              |""".stripMargin

        if (edit) {
          val path = Paths.get(Workspace.EditsDir.toString, s"${summary.reference}.yaml")
          val yaml = header + summary.yaml
          ws.writeText(path, yaml).void
        } else {
          Sync[F].delay(println(summary.yaml))
        }

      }

    def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
      ws.proposal(reference).flatMap { case (_, ps) => summarize(ws, ps) } .as(ExitCode.Success)

  }

}

