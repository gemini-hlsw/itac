package itac.config

import edu.gemini.model.p1.immutable.ClassicalProposalClass
import edu.gemini.model.p1.immutable.ExchangeProposalClass
import edu.gemini.model.p1.immutable.FastTurnaroundProgramClass
import edu.gemini.model.p1.immutable.LargeProgramClass
import edu.gemini.model.p1.immutable.Proposal
import edu.gemini.model.p1.immutable.ProposalClass
import edu.gemini.model.p1.immutable.QueueProposalClass
import edu.gemini.model.p1.immutable.SpecialProposalClass
import edu.gemini.model.p1.immutable.SubaruIntensiveProgramClass
import io.circe._
import io.circe.generic.semiauto._
import scalaz.{ Lens, State }
import edu.gemini.model.p1.immutable.Itac

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
