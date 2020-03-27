package itac

import cats._
import cats.implicits._
import edu.gemini.model.p1.immutable.Proposal
import io.chrisdavenport.log4cats.Logger
import edu.gemini.model.p1.immutable.GeminiNormalProposalClass
import java.io.File
import edu.gemini.model.p1.immutable.ExchangeProposalClass
import itac.config.Edit

/** Proposal editing. This applies edits that are supplied as part of the configuration. */
class Editor[F[_]: Applicative](edits: Map[String, Edit], log: Logger[F]) {
  import EditorOps._

  def applyEdits(file: File, p: Proposal): F[Proposal] =
    edits.get(p.id) match {
      case Some(f) => log.info(s"There are edits for ${p.id}/${file.getName} -- $f").as(f(p))
      case None    => log.trace(s"No edits for ${p.id}/${file.getName}").as(p)
    }

}

object EditorOps {

  implicit class ProposalOps(self: Proposal) {

    def id: String =
      self.proposalClass match {

        case pc: GeminiNormalProposalClass =>
          (pc.subs match {
            case Left(ss)  => ss.flatMap(_.response.map(_.receipt.id)).headOption
            case Right(s)  => s.response.map(_.receipt.id)
          }).getOrElse(sys.error(s"Can't get id from ${pc.subs}"))

        case pc: ExchangeProposalClass =>
          pc.subs.flatMap(_.response.map(_.receipt.id)).headOption.getOrElse(sys.error(s"Can't get id from ${pc.subs}"))

        case pc => sys.error(s"Unsupported proposal class: $pc")
      }

  }

}
