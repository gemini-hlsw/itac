// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.data.State
import cats.implicits._
import edu.gemini.tac.qengine.api.QueueCalc
import cats.data.NonEmptyList
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.spModel.core.ProgramId
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p1.Mode
import edu.gemini.tac.qengine.p1.Mode._
import edu.gemini.tac.qengine.ctx.Context
import edu.gemini.tac.qengine.log.ProposalLog

/** The final queue result, with joint proposal grouped and program IDs assigned. */
final case class QueueResult(bandedQueue: QueueBand => List[Proposal], context: Context, proposalLog: ProposalLog) {
  import QueueResult.Entry
  import context.{ site, semester }

  /** Group joints together by finding proposals with the same PI and title, sorting each group by rank. */
  private def groupJoints(ps: List[Proposal]): List[NonEmptyList[Proposal]] =
    ps.groupBy(p => (p.piName, p.p1proposal.title, p.too, p.p1proposal.proposalClass.getClass().getName())).values.toList.map(ps => NonEmptyList.fromList(ps.sortBy(_.ntac.ranking.num.getOrElse(0.0))).get)

  protected def programId(p: Proposal): State[(Int, Int), ProgramId] =
    State { case (cn, qn) =>

      def pid(num: Int): ProgramId =
        ProgramId.parse(s"${context.site.abbreviation}-${context.semester}-${p.mode.programId}-$num")

      p.mode match {
        case Classical => ((cn + 1, qn), pid(cn))
        case Queue     => ((cn, qn + 1), pid(qn))
        case LargeProgram =>
          p.ntac.reference.split("-") match {
            case Array("LP", _, num)    => ((cn, qn), pid(num.toInt))
            case Array("LP", _, num, _) => ((cn, qn), pid(num.toInt))
            case _ => sys.error(s"Proposal ${p.ntac.reference} doesn't have a valid LP reference.")
          }
        }

    }

  /** Get entries in the specified band, ordered by program id. */
  def entries(qb: QueueBand): List[Entry] = {
    val ps = bandedQueue(qb)
    val gs = groupJoints(ps).sortBy(_.head.piName.fold("")(_.reverse))
    val x = gs.traverse(nel => programId(nel.head).map(Entry(nel, _))) : State[(Int, Int), List[Entry]]
    x.runA((1, qb.number * 100 + 1)).value
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
    lazy val accepted = QueueBand.values.flatMap(bandedQueue).map(_.ntac.reference).toSet
    candidates
      .filter(p => p.site == site && p.mode == Mode.Queue)
      .filter(p => !accepted(p.ntac.reference))
  }

  def unsuccessful(candidates: List[Proposal], partner: Partner): List[Proposal] =
    unsuccessful(candidates).filter(_.ntac.partner == partner)

}

object QueueResult {

  // temp
  def apply(queueCalc: QueueCalc): QueueResult =
    apply(b => queueCalc.queue(b).toList, queueCalc.context, queueCalc.proposalLog)

  final case class Entry(proposals: NonEmptyList[Proposal], programId: ProgramId)

}
