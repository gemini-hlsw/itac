package edu.gemini.tac.qengine.impl.resource

import edu.gemini.tac.qengine.impl.block.Block
import edu.gemini.tac.qengine.log.RejectMessage
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import xml.Elem


class TimeResourceGroup(val lst: List[TimeResource]) extends Resource {
  type T = TimeResourceGroup

  def reserve(block: Block, queue: ProposalQueueBuilder): RejectMessage Either TimeResourceGroup =
    Resource.reserveAll(block, queue, lst).right map {
      lst => new TimeResourceGroup(lst)
    }

  def toXML : Elem = <TimeResourceGroup>
    { lst.map(_.toXML) }
    </TimeResourceGroup>
}