package edu.gemini.tac.qservice.impl.shutdown

import org.junit._
import Assert._
import edu.gemini.qengine.skycalc.RaBinSize
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.ctx.{Site, Semester, Context}
import org.joda.time._
import edu.gemini.tac.qengine.api.config

class ShutdownTest {
  val size60 = new RaBinSize(60)

  @Test def testCombineEmptyList() {
    assertEquals(List.fill(size60.getBinCount)(Time.ZeroHours), ShutdownCalc.sumHoursPerRa(Nil, size60))
  }

  @Test def testCombineSingleList() {
    val sd = new config.Shutdown(Site.south, new DateTime(2011, 5, 6, 14, 0).toDate, new DateTime(2011, 5, 7, 14, 0).toDate)
    val hrs = ShutdownCalc.sumHoursPerRa(List(sd), size60)
    assertEquals(ShutdownCalc.timePerRa(sd, size60), hrs)
  }

  @Test def testCombineMultipleLists() {
    val sd0 = new config.Shutdown(Site.south, new DateTime(2011, 5, 6, 14, 0).toDate, new DateTime(2011, 5, 7, 14, 0).toDate)
    val sd1 = new config.Shutdown(Site.south, new DateTime(2011, 5, 6, 14, 0).toDate, new DateTime(2011, 5, 7, 14, 0).toDate)
    val hrs = ShutdownCalc.sumHoursPerRa(List(sd0, sd1), size60)

    val hrs0 = ShutdownCalc.timePerRa(sd0, size60)
    val hrs1 = ShutdownCalc.timePerRa(sd1, size60)
    val sum = (hrs0 zip hrs1) map {
      case (h0, h1) => h0 + h1
    }

    assertEquals(sum, hrs)
  }

  @Test def compare(): Unit = {
    val sd0 = new config.Shutdown(Site.south, new DateTime(2011, 5, 6, 14, 0).toDate, new DateTime(2011, 5, 7, 14, 0).toDate)
    val sd1 = new config.Shutdown(Site.south, new DateTime(2011, 5, 6, 14, 0).toDate, new DateTime(2011, 5, 8, 14, 0).toDate)
    assertTrue(sd0 < sd1)
  }

  val ctx = new Context(Site.south, Semester.parse("2011A"))

  @Test def trim(): Unit = {

    val semStart = ctx.getSemester.getStartDate(ctx.getSite)
    val semEnd = ctx.getSemester.getEndDate(ctx.getSite)

    // Mismatch semesters.
    val mismatchSemesters =  new config.Shutdown(Site.north, new DateTime(2011, 5, 1, 14, 0).toDate, new DateTime(2011, 5, 2, 14, 0).toDate)
    ShutdownCalc.trim(mismatchSemesters, ctx) match {
      case None =>
      case _ => fail()
    }

    // Completely before.
    val before =   new config.Shutdown(Site.south, new DateTime(2011, 1, 1, 14, 0).toDate, new DateTime(2011, 1, 30, 14, 0).toDate) //extract("S 2011/1/1 2011/1/30")
    ShutdownCalc.trim(before, ctx) match {
      case None =>
      case _ => fail()
    }

    // Overlapping start.
    val overlapStart = new config.Shutdown(Site.south, new DateTime(2011, 1, 30, 14, 0).toDate, new DateTime(2011, 2, 15, 14, 0).toDate) //extract("S 2011/1/30 2011/2/15")
    ShutdownCalc.trim(overlapStart, ctx) match {
      case Some(sd) =>
        assertEquals(sd.start, semStart)
        assertEquals(sd.end, overlapStart.end)
      case _ => fail()
    }

    // Completely within.
    val within =  new config.Shutdown(Site.south, new DateTime(2011, 3, 15, 14, 0).toDate, new DateTime(2011, 3, 18, 14, 0).toDate) //extract("S 2011/3/15 2011/3/18")
    ShutdownCalc.trim(within, ctx) match {
      case Some(sd) => assertEquals(within, sd)
      case _ => fail()
    }

    // Overlapping end.
    val overlapEnd =  new config.Shutdown(Site.south, new DateTime(2011, 7, 28, 14, 0).toDate, new DateTime(2011, 8, 15, 14, 0).toDate) //extract("S 2011/7/28 2011/08/15")
    ShutdownCalc.trim(overlapEnd, ctx) match {
      case Some(sd) =>
        assertEquals(sd.start, overlapEnd.start)
        assertEquals(sd.end, semEnd)
      case _ => fail()
    }

    // Completely after.
    val after =  new config.Shutdown(Site.south, new DateTime(2011, 8, 15, 14, 0).toDate, new DateTime(2011, 8, 16, 14, 0).toDate) //extract("S 2011/08/15")
    ShutdownCalc.trim(after, ctx) match {
      case None =>
      case _ => fail()
    }

    // Giant shutdown that starts before and ends after the semester.
    val giant =  new config.Shutdown(Site.south, new DateTime(2010, 1, 1, 14, 0).toDate, new DateTime(2012, 1, 1, 14, 0).toDate)  //extract("S 210/1/1 212/1/1")
    ShutdownCalc.trim(giant, ctx) match {
      case Some(sd) =>
        assertEquals(sd.start, semStart)
        assertEquals(sd.end, semEnd)
      case _ => fail()
    }

    val lst = List(mismatchSemesters, before, overlapStart, within, overlapEnd, after, giant)
    ShutdownCalc.trim(lst, ctx) match {
      case List(tOverlapStart, tWithin, tOverlapEnd, tGiant) =>
        assertEquals(tOverlapStart.start, semStart)
        assertEquals(tOverlapStart.end, overlapStart.end)

        assertEquals(tWithin.start, within.start)
        assertEquals(tWithin.end, within.end)

        assertEquals(tOverlapEnd.start, overlapEnd.start)
        assertEquals(tOverlapEnd.end, semEnd)

        assertEquals(tGiant.start, semStart)
        assertEquals(tGiant.end, semEnd)
      case _ => fail()
    }
  }

  @Test def testValidateEmpty() {
    assertTrue(ShutdownCalc.validate(Nil))
  }

  @Test def testValidateOne() {
    val sd =  new config.Shutdown(Site.south, new DateTime(2011, 3, 4, 14, 0).toDate, new DateTime(2011, 3, 5, 14, 0).toDate) //extract("S 2011/3/4")
    assertTrue(ShutdownCalc.validate(List(sd)))
  }

  @Test def testValidateConsecutive() {
    val sd0 = new config.Shutdown(Site.south, new DateTime(2011, 3, 4, 14, 0).toDate, new DateTime(2011, 3, 5, 14, 0).toDate) //extract2("S 2011/3/4 2011/3/6 S 2011/3/6")
    val sd1 = new config.Shutdown(Site.south, new DateTime(2011, 3, 5, 14, 0).toDate, new DateTime(2011, 3, 6, 14, 0).toDate)
    assertTrue(ShutdownCalc.validate(List(sd0, sd1)))
    assertTrue(ShutdownCalc.validate(List(sd1, sd0)))
  }

  @Test def testValidateOverlaps() {
    val sd0 =  new config.Shutdown(Site.south, new DateTime(2011, 3, 4, 14, 0).toDate, new DateTime(2011, 3, 6, 14, 0).toDate) //extract("S 2011/3/4 2011/3/6")
    val sd1 =  new config.Shutdown(Site.south, new DateTime(2011, 3, 5, 14, 0).toDate, new DateTime(2011, 3, 6, 14, 0).toDate) //extract("S 2011/3/5")
    assertFalse(ShutdownCalc.validate(List(sd0, sd1)))
  }
}