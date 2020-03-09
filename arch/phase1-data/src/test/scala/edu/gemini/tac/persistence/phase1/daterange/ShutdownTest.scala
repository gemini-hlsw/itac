package edu.gemini.tac.persistence.phase1.daterange

import edu.gemini.tac.persistence.daterange.{DateRangePersister, Shutdown}
import edu.gemini.shared.util.DateRange
import java.util.Calendar
import org.joda.time.{Period, DateTime}
import org.junit.{Assert, Test}
import edu.gemini.tac.persistence.{Committee, Site}

class ShutdownTest {
  @Test
  def canInstantiate() {
    val start = new DateTime(2012, 3, 26, 12, 0, 0, 0);
    val stop = start.plus(Period.days(1));
    val dr = new DateRange(start.toDate, stop.toDate)
    val drp = new DateRangePersister(dr)
    val site = Site.NORTH
    val committee = new Committee()
    val s = new Shutdown(drp, site, committee)
    Assert.assertNotNull(s)
  }

  @Test
  def canGetFromCommittee() {
    val start = new DateTime(2012, 3, 26, 12, 0, 0, 0);
    val stop = start.plus(Period.days(1));
    val dr = new DateRange(start.toDate, stop.toDate)
    val drp = new DateRangePersister(dr)
    val site = Site.NORTH
    val committee = new Committee()
    val s = new Shutdown(drp, site, committee)
    committee.addShutdown(s)
    val ss = committee.getShutdowns
    Assert.assertEquals(1, ss.size())
  }
}