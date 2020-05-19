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

object Summarize {

  private implicit class ObservationOps(o: Observation) {
    def ra:  HourAngle = Angle.hourAngle.get(Angle.fromDoubleDegrees(o.target.ra.mag))
    def dec: Angle     = Angle.fromDoubleDegrees(o.target.dec.mag)
  }

  final case class BandedObservation(band: String, obs: Observation)

  final case class Field(name: String, order: Order[BandedObservation])
  object Field {

    val band = Field("band",  Order.by(o => o.band))
    val hash = Field("hash",  Order.by(o => ObservationDigest.digest(o.obs.p1Observation)))
    val ra   = Field("ra",    Order.by(o => o.obs.ra.toDoubleDegrees))
    val dec  = Field("dec",   Order.by(o => o.obs.dec.toSignedDoubleDegrees))
    val time = Field("time",  Order.by(o => o.obs.time.toHours.value))
    val name = Field("name",  Order.by(o => o.obs.target.name.orEmpty.toLowerCase()))

    val all: List[Field] =
      List(band, hash, ra, dec, time, name)

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

  def summary(ps: List[Proposal]): String =
    ???


  def apply[F[_]: Sync](reference: String, fields: NonEmptyList[Field]): Operation[F] =
    new Operation[F] {

      def summarize(ps: NonEmptyList[Proposal]): F[Unit] =
        Sync[F].delay {

          val order = fields.reduceMap(_.order)(Order.whenEqualMonoid)

          def obs(band: String)(o: Observation): String = {
            val id    = ObservationDigest.digest(o.p1Observation)
            val ra    = HourAngle.HMS(o.ra).format
            val dec   = Angle.DMS(o.dec).format
            val conds = f"${o.conditions.cc}%-5s ${o.conditions.iq}%-5s ${o.conditions.sb}%-5s ${o.conditions.wv}%-5s "
            val hrs   = o.time.toHours.value
            f"  - $id  $band%-4s  $hrs%5.1fh  $conds  $ra%16s  $dec%16s  ${o.target.name.orEmpty}"
          }

          def obsList(p: Proposal): List[BandedObservation] =
            p.obsList.map(BandedObservation("B1/2", _)) ++
            p.band3Observations.map(BandedObservation("B3", _))

          def obsYaml(p: Proposal): String =
            f"""|  ${p.site}:
                |${obsList(p).sorted(order.toOrdering).map(bo => obs(bo.band)(bo.obs)).mkString("\n")}
                |""".stripMargin

          val header =
            f"""|
                |# Edits for ${ps.head.id.reference}. Any changes you make here will be applied when the proposal is read.
                |# To discard edits, delete this file.
             """.stripMargin

          val yaml =
            f"""|
                |Reference: ${ps.head.id.reference}
                |Mode:      ${ps.head.mode}
                |Title:     ${ps.head.p1proposal.title}
                |PI:        ${ps.head.piName.orEmpty}
                |Partner:   ${ps.head.ntac.partner.fullName}
                |Award:     ${ps.head.time.toHours.value}%1.1f
                |Rank:      ${ps.head.ntac.ranking.num.orEmpty}%1.1f
                |ToO:       ${ps.head.too}
                |
                |Observations:
                |${ps.map(obsYaml).toList.mkString("")}
            """.stripMargin

          println(header + yaml)

        }

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        ws.proposal(reference).flatMap { case (_, ps) => summarize(ps) } .as(ExitCode.Success)

  }

}

