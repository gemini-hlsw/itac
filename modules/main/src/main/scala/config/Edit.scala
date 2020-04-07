// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.config

import itac.EditorOps
import edu.gemini.model.p1.immutable._
import io.circe._
import io.circe.generic.semiauto._
import itac.ObservationDigest
import scalaz.{ Lens, State }
import itac.ItacException
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.core.RightAscension
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.core.Declination
import java.util.UUID

/** We apply a multi-part edit to each proposal as it's loaded from disk. */
case class Edit(
  itacComment:  Option[String],
  observations: Option[Map[String, ObservationEdit]] // unusual type to support Yaml
) {
  import Edit.lenses._

  val obsEdits: Map[String, ObservationEdit] =
    observations.getOrElse(Map.empty)

  def apply(p: Proposal): Proposal = {
    import EditorOps._

    // Our new Itac element
    val newItac: Option[Itac] =
      Some(Itac(None, None, itacComment))

    // Our new Observation list
    val newObservations = {

      // Ensure every observation mentioned in the edit exists in this program.
      val all    = p.observations.map(ObservationDigest.digest).toSet
      val unused = obsEdits.keySet -- all
      if (unused.nonEmpty)
        throw new ItacException(s"Edit: ${p.id}: no such observation(s): ${unused.mkString(" ")}")

      // Replace or remove observations as specified by corresponding edits, if any.
      p.observations.flatMap { o =>
        obsEdits.get(ObservationDigest.digest(o)) match {
          case Some(ObservationEdit.Disable)    => Nil // other disabled obs have already been removed
          case Some(r: ObservationEdit.Replace) =>
            // val dif = o.target.get.coords(0L).get.angularDistance(r.target.coords)
            // println(s"DIFFERENCE IS $dif")
            // Do we need to add this to the target list as well? Seems like we do.
            List(o.copy(target = Some(r.target)))
          case None                             => List(o)
        }
      }

    }

    // State action to update everything.
    val update: State[Proposal, Unit] =
      for {
        _ <- Proposal.itac := newItac
        _ <- Proposal.observations := newObservations
      } yield ()

    // Run it to create the new Proposal.
    update.exec(p)

  }

}

object Edit {

  implicit val decoder: Decoder[Edit] = deriveDecoder

  object lenses {

    implicit class ProposalClassCompanionOps(self: ProposalClass.type) {
      val itac: Lens[ProposalClass, Option[Itac]] =
        Lens.lensu(
          (pc, op) => pc match {
            case pc: QueueProposalClass          => pc.copy(itac = op)
            case pc: ClassicalProposalClass      => pc.copy(itac = op)
            case pc: SpecialProposalClass        => pc.copy(itac = op)
            case pc: ExchangeProposalClass       => pc.copy(itac = op)
            case pc: LargeProgramClass           => pc.copy(itac = op)
            case pc: SubaruIntensiveProgramClass => pc.copy(itac = op)
            case pc: FastTurnaroundProgramClass  => pc.copy(itac = op)
          },
          _.itac
        )
    }

    implicit class ProposalCompanionOps(self: Proposal.type) {
      val itac: Lens[Proposal, Option[Itac]] =
        Proposal.proposalClass >=> ProposalClass.itac
    }

  }

}

sealed trait ObservationEdit
object ObservationEdit {

  case object Disable extends ObservationEdit
  case class  Replace(name: String, coords: Coordinates) extends ObservationEdit {
    def target: SiderealTarget =
      SiderealTarget(
        uuid         = UUID.randomUUID,
        name         = this.name,
        coords       = this.coords,
        epoch        = CoordinatesEpoch.J_2000,
        properMotion = None,
        magnitudes   = Nil
      )
  }

  val DecoderReplace: Decoder[ObservationEdit] =
    Decoder[String].emap { s =>
      s.split("\\s+", 4) match {
        case Array("Replace", hms, dms, name) =>

          val r: Either[String, Replace] =
            for {
              ra  <- Angle.parseHMS(hms).leftMap(_.getMessage).toEither.map(RightAscension.fromAngle)
              dec <- Angle.parseDMS(dms).leftMap(_.getMessage).toEither.flatMap(Declination.fromAngle(_).toRight("Invalid declination."))
            } yield Replace(name, Coordinates(ra, dec))

          r match {
            case Right(r) => Right(r)
            case Left(m) => Left(s"Invalid coordinates: $hms $dms -- $m")
          }

        case _ => Left(s"Not a valid target: $s\nExpected: Replace <hms> <dms> <name>")
      }
    }

  val DecoderDisable: Decoder[ObservationEdit] =
    Decoder[String].map(_.toLowerCase).emap {
      case "disable" => Right(Disable)
      case s         => Left(s"Not a valid observation edit: $s")
    }

  implicit val DecodeObservationEdit: Decoder[ObservationEdit] =
    DecoderDisable or DecoderReplace

}

