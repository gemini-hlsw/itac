// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.implicits._
import edu.gemini.tac.qengine.api.QueueCalc
import cats.data.NonEmptyList
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.spModel.core.ProgramId
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.ctx.Partner

/** The final queue result, with joint proposal grouped and program IDs assigned. */
final case class QueueResult(queueCalc: QueueCalc) {
  import QueueResult.Entry
  import queueCalc.context.{ site, semester }

  /** Sort by PI last name, reversed, using TAC references as a tie-breaker. */
  private def shuffle(ps: List[NonEmptyList[Proposal]]): List[NonEmptyList[Proposal]] =
    ps.sortBy { nel => (nel.head.piName.orEmpty.reverse, nel.head.id.reference) }

  /** Group joints together by finding proposals with the same PI and title. */
  private def groupJoints(ps: List[Proposal]): List[NonEmptyList[Proposal]] =
    ps.groupBy(p => (p.piName, p.p1proposal.title)).values.toList.map(NonEmptyList.fromList(_).get)

  /** Get entries in the specified band, ordered by program id. */
  def entries(qb: QueueBand): List[Entry] = {
    val ps = queueCalc.queue.bandedQueue.getOrElse(qb, Nil)
    val gs = shuffle(groupJoints(ps))
    gs.zipWithIndex.map { case (nel, n) =>
      Entry(nel, ProgramId.parse(s"${site.abbreviation}-${semester}-${nel.head.mode.programId}-${100 * qb.number + (n + 1)}"))
    }
  }

  /** Get entries in the specified band, per partner, ordered by program id. */
  def entries(qb: QueueBand, partner: Partner): List[Entry] =
    entries(qb).mapFilter { e =>
      e.proposals
       .filter(_.ntac.partner == partner)
       .toNel
       .map(Entry(_, e.programId))
    }

}

object QueueResult {

  final case class Entry(proposals: NonEmptyList[Proposal], programId: ProgramId)

}