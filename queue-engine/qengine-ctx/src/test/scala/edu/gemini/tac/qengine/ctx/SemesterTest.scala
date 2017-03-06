package edu.gemini.tac.qengine.ctx

import org.junit._
import Assert._

import Semester.Half._
import java.text.ParseException
import java.util.{Date, GregorianCalendar, Calendar}

class SemesterTest {
  val site = Site.south

  private def mkCal: Calendar = {
    val cal = new GregorianCalendar(site.timeZone)
    cal.set(Calendar.MILLISECOND, 0)
    cal
  }

  private def verifySem(year: Int, half: Semester.Half, sem: Semester) {
    assertEquals(year, sem.getYear)
    assertEquals(half, sem.getHalf)
  }

  private def verifyDate(year: Int, month: Int, expected: Date) {
    val c = mkCal
    c.set(year, month, 1, 14, 0, 0)
    c.add(Calendar.DAY_OF_MONTH, -1)
    assertEquals(expected, c.getTime)
  }

  @Test def testExplicitConstruction() {
    val sem = new Semester(2010, B)
    verifySem(2010, B, sem)
    assertEquals("2010B", sem.toString)

    val start = sem.getStartDate(site)
    verifyDate(2010, B.getStartMonth, start)
    val end   = sem.getEndDate(site)
    verifyDate(2011, (B.getStartMonth + 6)%12, end)
  }

  @Test def testDateConstruction() {
    // Create a date solidly in the middle of 2010A
    val c = mkCal
    c.set(2010, A.getStartMonth + 3, 1, 0, 0, 0)
    verifySem(2010, A, new Semester(site, c.getTime))
  }

  @Test def testStartDateCreation() {
    val c = mkCal
    c.set(2010, A.getStartMonth, 1, 0, 0, 0)
    verifySem(2010, A, new Semester(site, c.getTime))
  }

  @Test def testEndDateCreation() {
    val c = mkCal
    c.set(2010, A.getStartMonth + 6, 1, 0, 0, 0)
    verifySem(2010, B, new Semester(site, c.getTime))
  }

  @Test def testJustBeforeEndDateCreation() {
    val c = mkCal
    c.set(2010, B.getStartMonth, 1, 14, 0, 0)
    c.add(Calendar.DAY_OF_YEAR, -1)
    verifySem(2010, B, new Semester(site, c.getTime))
    c.add(Calendar.MILLISECOND, -1)
    verifySem(2010, A, new Semester(site, c.getTime))
  }

  @Test def testParseGood() {
    assertEquals(new Semester(2007, B), Semester.parse("2007B"))
    assertEquals(new Semester(2010, A), Semester.parse("2010-A"))
    assertEquals(new Semester(   0, A), Semester.parse("0000A"))
  }

  @Test def testParseBad() {
    val bad = List("207B", "20007B", "2007", "-2007B", "2007C", "2007--A")

    bad.foreach {
      b => try {
        Semester.parse(b)
        fail()
      } catch {
        case ex: ParseException => // ok
      }
    }
  }

  val s2010A = new Semester(2010, A)
  val s2010B = new Semester(2010, B)

  @Test def testNextSemesterAfterAHasSameYear() {
    val s = s2010A.next
    assertEquals(2010, s.getYear)
    assertEquals(B,    s.getHalf)
  }

  @Test def testNextSemesterAfterBIsNextYear() {
    val s = s2010B.next
    assertEquals(2011, s.getYear)
    assertEquals(A,    s.getHalf)
  }

  @Test def testPreviousSemesterBeforeAHasPreviousYear() {
    val s = s2010A.previous
    assertEquals(2009, s.getYear)
    assertEquals(B,    s.getHalf)
  }

  @Test def testPreviousSemesterBeforeBHasSameYear() {
    val s = s2010B.previous
    assertEquals(2010, s.getYear)
    assertEquals(A,    s.getHalf)
  }

  @Test def testMidpoint(){
    val s = s2010A
    val d = new Date(1272801600000L)    //2010-05-02T02:00:00.000-1000
    assertEquals(d, s.getMidpoint(Site.north))
  }
}