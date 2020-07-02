package edu.gemini.tac.qengine.api.queue.time

import edu.gemini.spModel.core.Site
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1.{Mode, Proposal}
import edu.gemini.tac.qengine.ctx.Partner
import java.util.logging.{Level, Logger}

/**
 * Contains calculations of various PartnerTime categories that go into the
 * final QueueTime.
 */
object PartnerTimeCalc {
  val Log = Logger.getLogger("edu.gemini.itac")

  /**
   * Extracts the site's classical proposals and sums their times, associating
   * totals with each partner.
   */
  def classical(site: Site, props: List[Proposal], partners: List[Partner]): PartnerTime = {
    // Get the classical proposals for this site.
    val filteredProps  = props filter {
      prop => (prop.mode == Mode.Classical) && (prop.site == site)
    }

    // Group by partner, turning it into a Map[Partner, List[Proposal]]
    val cmap = filteredProps.groupBy(_.ntac.partner)

     // Sum the awarded times, converting the map into a Map[Partner, Time]
    def sumtime(t: Time, p: Proposal): Time = p.ntac.awardedTime + t
    val timemap = cmap.mapValues { plst => plst.foldLeft(Time.ZeroHours)(sumtime) }

    // Create a PartnerTime from the map.
    PartnerTime(partners, timemap)
  }

  /**
   * Computes the net queue time, which is
   * <code>base - (classical + large program + rollover + exchange)</code>
   * where negative times are left at zero.
   */
  def net(base: PartnerTime, partners: List[Partner],  deductions: PartnerTime*): PartnerTime = {
    val totalDeductions = deductions.foldLeft(PartnerTime.empty(partners))(_ + _)
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

