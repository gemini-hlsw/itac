package edu.gemini.tac.qengine.impl

import edu.gemini.tac.qengine.p1.Proposal

/**
 * Encapsulates code for calculating the poor weather queue.
 */
object PoorWeatherCalc {

  def apply(candidates: List[Proposal]): List[Proposal] =
    candidates.filter(_.isPoorWeather).sortBy(_.id)
}