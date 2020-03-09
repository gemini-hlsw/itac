// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.impl.resource

import edu.gemini.tac.qengine.impl.block.Block
import edu.gemini.tac.qengine.log.RejectMessage
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import xml.Elem
import org.slf4j.LoggerFactory

class TimeResourceGroup(val lst: List[TimeResource]) extends Resource {
  type T = TimeResourceGroup
  val LOG = LoggerFactory.getLogger(getClass)

  def reserve(block: Block, queue: ProposalQueueBuilder): RejectMessage Either TimeResourceGroup =
    Resource.reserveAll(block, queue, lst) match {
      case Right(resource) =>
        LOG.debug(s"    💚  I was able to meet bin restrictions.")
        Right(new TimeResourceGroup(resource))
      case Left(err) =>
        LOG.debug(s"    ❌  I was unable to meet bin restrictions")
        Left(err)
    }

  def toXML: Elem = <TimeResourceGroup>
    {lst.map(_.toXML)}
    </TimeResourceGroup>
}
