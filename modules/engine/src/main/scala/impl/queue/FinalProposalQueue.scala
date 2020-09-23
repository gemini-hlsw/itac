package edu.gemini.tac.qengine.impl.queue

import edu.gemini.tac.qengine.p1.{QueueBand, Proposal}
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.api.queue.ProposalPosition

/**
 * A ProposalQueue implementation that contains a final queue calculation.
 */
class FinalProposalQueue(val queueTime: QueueTime, bandMap: Map[QueueBand, List[Proposal]]) extends edu.gemini.tac.qengine.api.queue.ProposalQueue {

  // Complete the map with empty lists for bands that weren't present.
  val bandedQueue: Map[QueueBand, List[Proposal]] =
    QueueBand.values.map(band => band -> bandMap.getOrElse(band, Nil)).toMap

  val usedTime: Time = Proposal.sumTimes(bandedQueue.values.flatten)

  def toList: List[Proposal] =
    (QueueBand.values map { band => bandedQueue(band) }).flatten

  private def zipBandWithPosition(index: Int, time: Time, band: QueueBand, props: List[Proposal]): (List[(Proposal, ProposalPosition)], Int, Time) = {
    def pos = ProposalPosition(index, time, band, 0, Time.ZeroHours)
    val posList = props.scanLeft(pos) { case (pos, prop) => pos.next(prop.time, band) }
    val lastPos = posList.last
    (props.zip(posList), lastPos.index, lastPos.time)
  }

  def zipWithPosition: List[(Proposal, ProposalPosition)] = {
    val bandedList  = bandedQueue.toList.sortBy { case (band,_) => band.number }
    val emptyZipped = List.empty[(Proposal, ProposalPosition)]

    val (res, _, _) = bandedList.foldLeft((emptyZipped, 0, Time.ZeroHours)) {
      case ((curZipped, startIndex, startTime), (band, propList)) =>
        val (bandZipped, endIndex, endTime) = zipBandWithPosition(startIndex, startTime, band, propList)
        (curZipped ++ bandZipped, endIndex, endTime)
    }
    res
  }

  def positionOf(prop: Proposal): Option[ProposalPosition] =
    (zipWithPosition.find { case (curProp, _) => curProp.id == prop.id }) map {
      case (_, pos) => pos
    }
}