package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.{QueueBand, Observation}

/**
 * A proposal rejection that occurs because a particular observation in the
 * proposal was rejected.  Identifies the observation in question and provides
 * some information about the percent of observations (and time) that were
 * successfully merged prior to the the rejection.
 */
trait ObsRejectMessage extends RejectMessage {
  def obs: Observation

  def percentObsMerged(b: QueueBand): Int = {
    val l = prop.obsListFor(b)
    ((l.indexOf(obs) / l.size.toDouble) * 100).round.toInt
  }

  def percentTimeMerged(b: QueueBand): Int = {
    val totalTime = (0l/:prop.obsListFor(b))(_ + _.time.ms)
    ((obs.time.ms / totalTime.toDouble) * 100).round.toInt
  }
}