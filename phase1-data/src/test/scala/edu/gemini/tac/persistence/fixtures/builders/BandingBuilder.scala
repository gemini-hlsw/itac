package edu.gemini.tac.persistence.fixtures.builders

import edu.gemini.tac.persistence.Proposal
import edu.gemini.tac.persistence.phase1.TimeAmount
import edu.gemini.tac.persistence.queues.{Banding, JointBanding, ScienceBand, Queue}

/**
 * Mock Bandings
 *
 */

abstract class BandingBuilder(
                               val queue : Queue, val proposal : Proposal, val band : ScienceBand,
                               val mergeIndex : Int, val awardedTime : TimeAmount,  val programId : String,
                               val maybeJointBanding : Option[JointBanding] = None) {

}

object BandingBuilder {
  def apply(queue : Queue, proposal : Proposal, band : ScienceBand,
                               mergeIndex : Int, awardedTime : TimeAmount,  programId : String,
                               maybeJointBanding : Option[JointBanding] = None) : Banding = {
    val banding = new Banding(queue, proposal, band);

    banding;

  }
}