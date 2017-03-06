package edu.gemini.tac.qengine.api.queue.time

import edu.gemini.tac.qengine.ctx.Site
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1.{Mode, Proposal}
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p2.rollover.RolloverReport
import java.util.logging.{Level, Logger}

import scala.collection.JavaConverters._

/**
 * Contains calculations of various PartnerTime categories that go into the
 * final QueueTime.
 */
object PartnerTimeCalc {
  val Log = Logger.getLogger(this.getClass.getName)

  /**
   * Distributes the total queue over the partners for the given site according
   * to their percentage shares.
   */
  def base(site: Site, totalQueueTime: Time, partners: List[Partner]): PartnerTime =
    PartnerTime.distribute(totalQueueTime.toHours, site, partners)


  /**
   * Extracts the site's classical proposals and sums their times, associating
   * totals with each partner.
   */
  def classical(site: Site, props: List[Proposal], partners: List[Partner]): PartnerTime = {
    // Get the classical proposals for this site.
    val filteredProps  = props filter {
      prop => (prop.mode == Mode.Classical) && (prop.site == site)
    }

    // Expand joints into their parts.
    val cprops = Proposal.expandJoints(filteredProps)

    // Group by partner, turning it into a Map[Partner, List[Proposal]]
    val cmap = cprops.groupBy(_.ntac.partner)

     // Sum the awarded times, converting the map into a Map[Partner, Time]
    def sumtime(t: Time, p: Proposal): Time = p.ntac.awardedTime + t
    val timemap = cmap.mapValues { plst => (Time.ZeroHours/:plst)(sumtime) }

    // Create a PartnerTime from the map.
    PartnerTime(partners, timemap)
  }

  /**
   * PartnerTime corresponding to the distribution of rollover time over the
   * partners who participate in the site.
   *
   * <p>As stated in the 11A Software Requirements (v5, 11A-TAC-19):
   * All observations containing non-zero planned time that are contained in
   * the report will be charged against the total queue time. Partners are not
   * individually deducted time for rollover proposals.</p>
   *
   * <p>For this reason, the time is distributed across partners evenly by this
   * calculation.</p>
   */
  def rollover(site: Site, rop: RolloverReport, partners: List[Partner]): PartnerTime =
    PartnerTime.distribute(rop.filter(site).total, site, partners)


  /**
   * Computes the net queue time, which is
   * <code>base - (classical + large program + rollover + exchange)</code>
   * where negative times are left at zero.
   */
  def net(base: PartnerTime, partners: List[Partner],  deductions: PartnerTime*): PartnerTime = {
    val totalDeductions = (PartnerTime.empty(partners) /:deductions)(_ + _)
    val tmp = base - totalDeductions
    tmp.mapTimes((_: Partner, t: Time) => Time.max(Time.ZeroHours, t))
  }

}

import PartnerTimeCalc.Log

/**
 * Container for named PartnerTime values.
 */
case class PartnerTimeCalc(partners: List[Partner],
                           base: PartnerTime,
                           classical: PartnerTime,
                           rollover: PartnerTime,
                           exchange: PartnerTime,
                           adjustment: PartnerTime,
                           partnerTrade: PartnerTime) {
  Log.log(Level.FINE, "PartnerTimeCalc.base = " + base.toString)

  val net: PartnerTime = PartnerTimeCalc.net(base, partners, classical, rollover, exchange, adjustment, partnerTrade)
  Log.log(Level.FINE, "PartnerTimeCalc.net = " + net.toString)
}

