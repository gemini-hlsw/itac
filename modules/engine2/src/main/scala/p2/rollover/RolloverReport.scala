package edu.gemini.tac.qengine.p2.rollover

import edu.gemini.tac.qengine.ctx.Site
import edu.gemini.tac.qengine.util.Time

/**
 * A collection of rollover observations whose time values should be deducted
 * from the corresponding bins.
 */
case class RolloverReport(obsList: List[RolloverObservation]) {

  /**
   * Filters the report for a particular site.
   */
  def filter(site: Site): RolloverReport =
    RolloverReport(obsList.filter(_.site == site))

  /**
   * Total of all rollover observation times.  This amount of time is subtracted
   * from the available queue time.
   */
  def total: Time = (Time.ZeroHours/:obsList)(_ + _.time)
}

object RolloverReport {
  /** An empty rollover report. */
  val empty: RolloverReport = RolloverReport(Nil)
}
