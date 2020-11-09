// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.effect.Sync
import cats.implicits._
import edu.gemini.model.p1.mutable.Proposal
import edu.gemini.model.p1.{ immutable => im }
import io.chrisdavenport.log4cats.Logger
import java.io.File

/** Proposal editing. This applies edits that are supplied as part of the configuration. */
class Editor[F[_]: Sync: Logger](edits: Map[String, SummaryEdit], log: Logger[F]) {
  import EditorOps._

  def applyEdits(file: File, p: Proposal): F[Unit] =
    p.id.flatMap(edits.get) match {
      case Some(e) =>

        log.debug(s"There are edits for ${p.id}/${file.getName}") *>
        e.applyUpdate(p)

      case None    => log.debug(s"No edits for ${p.id}/${file.getName}")

    }

}

object EditorOps {

  implicit class MutableProposalOps(self: Proposal) {
    def id = im.Proposal(self).id
  }

  implicit class ProposalOps(self: im.Proposal) {

    def id: Option[String] =
      self.proposalClass match {

        case pc: im.GeminiNormalProposalClass =>
          pc.subs match {
            case Left(ss)  => ss.flatMap(_.response.map(_.receipt.id)).headOption
            case Right(s)  => s.response.map(_.receipt.id)
          }

        case pc: im.ExchangeProposalClass =>
          pc.subs.flatMap(_.response.map(_.receipt.id)).headOption

        case lp: im.LargeProgramClass =>
          lp.sub.response.map(_.receipt.id).headOption

        case sp: im.SpecialProposalClass =>
          sp.sub.response.map(_.receipt.id).headOption

        case pc => sys.error(s"Unsupported proposal class: $pc")
      }

  }

}
