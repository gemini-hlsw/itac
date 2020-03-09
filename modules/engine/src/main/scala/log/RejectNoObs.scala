// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.Proposal
import xml.Elem

/**
 * A proposal rejection message for proposals that contain no observations.
 */
object RejectNoObs {
  val name        = "No Observations"
  val description = "Proposal contains no observations."
}
final case class RejectNoObs(prop: Proposal) extends RejectMessage {
  def reason: String = RejectNoObs.name
  def detail: String = RejectNoObs.description

  override def subToXML: Elem = <NoObs><Proposal id= {prop.id.toString}/></NoObs>
}
