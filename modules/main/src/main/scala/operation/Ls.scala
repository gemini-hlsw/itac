// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats._
import cats.effect.ExitCode
import cats.implicits._
import org.typelevel.log4cats.Logger
import itac.Workspace
import itac.Operation
import cats.effect.Sync
import edu.gemini.tac.qengine.p1.Proposal
import cats.effect.Blocker
import cats.data.NonEmptyList
import edu.gemini.tac.qengine.ctx.Partner
import itac.ItacException
import itac.util.Colors

object Ls {

  def apply[F[_]: Sync: Parallel](fields: NonEmptyList[Field], partnerNames: List[String]): Operation[F] =
    new Operation[F] {

      val order = fields.reduceMap(_.order)(Order.whenEqualMonoid)

      def header: String =
        f"${Colors.BOLD}${"Id"}%-18s  Site    ${"PI"}%-20s  ${"Rank"}%4s ${"Partner"}%6s   ${"Time"}%6s${Colors.RESET}"

      def format(p: Proposal): String =
        f"${p.id.reference}%-20s  ${p.site.abbreviation}    ${p.piName.orEmpty}%-20s  ${p.ntac.ranking}%4s  ${p.ntac.partner.id}%6s  ${p.ntac.awardedTime.toHours.value}%5.1f h"

      def findPartner(name: String): F[Partner] =
        Partner.all.find(_.id.equalsIgnoreCase(name)) match {
          case Some(p) => p.pure[F]
          case None    => new ItacException(s"No such partner: $name. Try one or more of ${Partner.all.map(_.id).mkString(",")}").raiseError[F, Partner]
        }

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] = {
        for {
          ns <- if (partnerNames.isEmpty) Partner.all.pure[F]
                else partnerNames.traverse(findPartner)
          ps <- ws.proposals.map(_.filter(p => ns.toSet(p.ntac.partner)).sorted(order.toOrdering))
          _  <- Sync[F].delay(println(header))
          _  <- ps.traverse_(p => Sync[F].delay(println(format(p))))
        } yield ExitCode.Success
      }

  }

  final case class Field(name: String, order: Order[Proposal])
  object Field {

    val id      = Field("id",       Order.by(p => p.id.reference))
    val site    = Field("site",     Order.by(p => p.site.abbreviation))
    val pi      = Field("pi",       Order.by(p => p.piName.orEmpty))
    val rank    = Field("rank",     Order.by(p => p.ntac.ranking.num.orEmpty))
    val partner = Field("partner",  Order.by(p => p.ntac.partner.id))
    val time    = Field("time",     Order.by(p => p.time.ms))

    val all: List[Field] =
      List(id, site, pi, rank, partner, time)

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

}