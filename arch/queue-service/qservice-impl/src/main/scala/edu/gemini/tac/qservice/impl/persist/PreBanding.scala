package edu.gemini.tac.qservice.impl.persist

import edu.gemini.tac.qengine.p2.ProgramId
import edu.gemini.tac.qengine.util.CompoundOrdering
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.api.queue.{ProposalQueue, ProposalPosition}
import edu.gemini.tac.qengine.ctx.Context

/**
 * A grouping of information required to make a persistence Banding.
 */
case class PreBanding(prop: Proposal, pos: ProposalPosition, progId: ProgramId)

object PreBanding {
  private implicit object ProgramIdOrdering extends CompoundOrdering(
    Ordering.by[(Proposal, ProposalPosition), Int](_._2.band.number),
    Ordering.by[(Proposal, ProposalPosition), String](_._1.piName.getOrElse("")).reverse
  )

  def toList(ctx: Context, propQueue: ProposalQueue): List[PreBanding] = {
    def toPreBanding(l: List[(Proposal, ProposalPosition)]): List[PreBanding] =
      l.zipWithIndex.map {
        case ((prop, propPos), progIndex) =>
          PreBanding(prop, propPos, ProgramId(ctx.getSite, ctx.getSemester, prop.mode, progIndex+1))
      }

    val modeGroups = propQueue.zipWithPosition.sorted.groupBy {
      case (prop, _) => prop.mode
    }

    modeGroups.toList.sortBy(_._1).unzip._2.map(toPreBanding).flatten
  }
}