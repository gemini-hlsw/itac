package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.{QueueBand, Proposal}

/**
 * A proposal rejection message for proposals that are remaining when all
 * available queue time has been filled for a phase of the algorithm.
 */
object RejectCategoryOverAllocation {
  val name  = "Band Time Limit"

  private val detailTemplate = "All queue time in %s has been allocated."
  def detail(cat: QueueBand.Category): String =
    detailTemplate.format(cat)
}

case class RejectCategoryOverAllocation(prop: Proposal, cat: QueueBand.Category) extends RejectMessage {
  def reason: String = RejectCategoryOverAllocation.name
  def detail: String = RejectCategoryOverAllocation.detail(cat)
}