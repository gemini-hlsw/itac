// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.{Proposal}
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.util.{Time, BoundedTime}

/**
 * A proposal rejection message for proposals of partners that have already
 * been allocated all the time that they were proportioned.
 */
object RejectPartnerOverAllocation {
  val name = "Partner Time Limit"

  private val fullTemplate = "%s already past %d%% allocation limit %s"
  private val longTemplate = "%.1f hr %s proposal would allocate too far beyond %d%% limit: %s"
  def detail(p: Partner, t: Time, guaranteed: BoundedTime, all: BoundedTime): String = {
    val perc = (guaranteed.limit.toHours.value / all.limit.toHours.value * 100.0).toInt
    if (guaranteed.isFull)
      fullTemplate.format(p.id, perc, LogMessage.formatBoundedTime(all))
    else {
      val s = all.overbook(t) map { b =>
        LogMessage.formatBoundedTime(b)
      } getOrElse ""
      longTemplate.format(t.toHours.value, p.id, perc, s)
    }
  }
}

case class RejectPartnerOverAllocation(prop: Proposal, guaranteed: BoundedTime, all: BoundedTime)
    extends RejectMessage {
  def reason: String = RejectPartnerOverAllocation.name
  def detail: String =
    RejectPartnerOverAllocation.detail(prop.ntac.partner, prop.time, guaranteed, all)
}
