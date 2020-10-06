package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.Proposal

case class RemovedRejectMessage(prop: Proposal) extends RejectMessage {
  def reason: String = "Unknown."
  def detail: String = "Proposal was removed from consideration."
}

