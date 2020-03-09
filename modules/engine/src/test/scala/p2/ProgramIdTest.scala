package edu.gemini.tac.qengine.p2

import edu.gemini.tac.qengine.p1.Mode

import org.junit._
import Assert._
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.core.Semester

class ProgramIdTest {

  @Test def testNegativeProgramIndicesNotAllowed() {
    try {
      ProgramId(Site.GN, Semester.parse("2011A"), Mode.Queue, -1)
      fail()
    } catch {
      case ex: IllegalArgumentException => // ok
    }
  }

  private def assertLessThan(p1: ProgramId, p2: ProgramId) {
    assertTrue(p1 < p2)
    assertFalse(p1 == p2)
    assertFalse(p2 < p1)
  }

  @Test def testSiteOrderingFirst() {
    val p1 = ProgramId(Site.GN, Semester.parse("2011B"), Mode.Queue, 99)
    val p2 = ProgramId(Site.GS, Semester.parse("2010A"), Mode.Classical, 1)
    assertLessThan(p1, p2)
  }

  @Test def testSemesterOrderingSecond() {
    val p1 = ProgramId(Site.GN, Semester.parse("2010A"), Mode.Queue, 99)
    val p2 = ProgramId(Site.GN, Semester.parse("2010B"), Mode.Classical, 1)
    assertLessThan(p1, p2)
  }

  @Test def testModeOrderingThird() {
    val p1 = ProgramId(Site.GN, Semester.parse("2010A"), Mode.Classical, 99)
    val p2 = ProgramId(Site.GN, Semester.parse("2010A"), Mode.Queue, 1)
    val p3 = ProgramId(Site.GN, Semester.parse("2010A"), Mode.LargeProgram, 9)
    assertLessThan(p1, p2)
    assertLessThan(p1, p3)
    assertLessThan(p3, p2)
  }

  @Test def testIndexOrderingForth() {
    val p1 = ProgramId(Site.GN, Semester.parse("2010A"), Mode.Queue, 1)
    val p2 = ProgramId(Site.GN, Semester.parse("2010A"), Mode.Queue, 99)
    assertLessThan(p1, p2)
  }

  @Test def testSomeStringsThatDontParse() {
    val nones = List(
      "",
      "GN",
      "GN-2011A",
      "GN-2011A-Q",
      "GX-2011A-Q-8",
      "GN-211A-Q-8",
      "GN-2011C-Q-8",
      "GN-2011A-X-8",
      "GN-2011A-Q--8"
    )

    nones.foreach(n => assertEquals(None, ProgramId.parse(n)))
  }

  @Test def testSomeStringsThatParse() {
    val ids = for {
      site     <- List(Site.GN, Site.GS)
      year     <- 2010 to 2011
      half     <- List(Semester.Half.A, Semester.Half.B)
      semester = new Semester(year, half)
      mode     <- Mode.All
      index    <- 1 to 10
    } yield ProgramId(site, semester, mode, index)

    ids.foreach { id =>
      assertEquals(Some(id), ProgramId.parse(id.toString))
    }

  }
}
