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
          case Some(ObservationEdit.Disable) => Nil // others have already been removed
          case None                          => List(o)
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

  implicit val DecodeObservationEdit: Decoder[ObservationEdit] =
    Decoder[String].map(_.toLowerCase).emap {
      case "disable" => Right(Disable)
      case s         => Left(s"Not a valid observation edit: $s")
    }

}