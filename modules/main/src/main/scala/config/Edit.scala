package itac.config

import edu.gemini.model.p1.immutable._
import io.circe._
import io.circe.generic.semiauto._
import scalaz.{ Lens, State } // because we're composing with existing lenses in the p1 model

/** We apply a multi-part edit to each proposal as it's loaded from disk. */
case class Edit(
  itacComment: Option[String]
) extends (Proposal => Proposal) {
  import Edit.lenses._

  def apply(p: Proposal): Proposal = {

    val s: State[Proposal, Unit] =
      for {
        _ <- Proposal.itac := Some(Itac(None, None, itacComment))
      } yield ()

    s.exec(p)

  }

}

object Edit {

  implicit val encoder: Encoder[Edit] = deriveEncoder
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
