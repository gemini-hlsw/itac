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
import edu.gemini.tac.qengine.p1.Mode
import edu.gemini.tac.qengine.ctx.Context
import edu.gemini.tac.qengine.log.ProposalLog

/** The final queue result, with joint proposal grouped and program IDs assigned. */
final case class QueueResult(bandedQueue: Map[QueueBand, List[Proposal]], context: Context, proposalLog: ProposalLog) {
  import QueueResult.Entry
  import context.{ site, semester }

  /** Group joints together by finding proposals with the same PI and title, sorting each group by rank. */
  private def groupJoints(ps: List[Proposal]): List[NonEmptyList[Proposal]] =
    ps.groupBy(p => (p.piName, p.p1proposal.title)).values.toList.map(ps => NonEmptyList.fromList(ps.sortBy(_.ntac.ranking.num.getOrElse(0.0))).get)

  /** Get entries in the specified band, ordered by program id. */
  def entries(qb: QueueBand): List[Entry] = {
    val ps = bandedQueue.getOrElse(qb, Nil)
    val gs = groupJoints(ps).sortBy(_.head.piName.fold("")(_.reverse))
    gs.zipWithIndex.map { case (nel, n) =>
      val num = explicitNumber(nel.head.ntac.reference).getOrElse(100 * qb.number + (n + 1))
      Entry(nel, ProgramId.parse(s"${site.abbreviation}-${semester}-${nel.head.mode.programId}-$num"))
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

  def classical(candidates: List[Proposal]): List[Entry] = // no support for joints, may not be needed
    candidates
      .filter(p => p.site == site && p.mode == Mode.Classical)
      .sortBy(p => (p.ntac.ranking.num.orEmpty, p.piName.foldMap(_.reverse)))
      .zipWithIndex
      .map { case (p, n) =>
        Entry(NonEmptyList.of(p), ProgramId.parse(s"${site.abbreviation}-${semester}-${p.mode.programId}-${n + 1}"))
      }

  def classical(candidates: List[Proposal], partner: Partner): List[Entry] =
    classical(candidates).mapFilter { e =>
      e.proposals
       .filter(_.ntac.partner == partner)
       .toNel
       .map(Entry(_, e.programId))
    }

  def unsuccessful(candidates: List[Proposal]): List[Proposal] = {
    lazy val accepted = bandedQueue.values.toList.flatten.map(_.ntac.reference).toSet
    candidates
      .filter(p => p.site == site && p.mode == Mode.Queue)
      .filter(p => !accepted(p.ntac.reference))
  }

  def unsuccessful(candidates: List[Proposal], partner: Partner): List[Proposal] =
    unsuccessful(candidates).filter(_.ntac.partner == partner)

  /** LP program numbers are given in the reference. */
  private def explicitNumber(ref: String): Option[Int] =
    ref.split("-") match {
      case Array("LP", _, num)    => Either.catchOnly[NumberFormatException](num.toInt).toOption
      case Array("LP", _, num, _) => Either.catchOnly[NumberFormatException](num.toInt).toOption
      case _ => None
    }

}

object QueueResult {

  // temo
  def apply(queueCalc: QueueCalc): QueueResult =
    apply(queueCalc.queue.bandedQueue, queueCalc.context, queueCalc.proposalLog)

  final case class Entry(proposals: NonEmptyList[Proposal], programId: ProgramId)

}
