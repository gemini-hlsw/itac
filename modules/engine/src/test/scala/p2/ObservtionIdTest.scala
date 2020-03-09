package edu.gemini.tac.qengine.p2

import org.junit._
import Assert._

import edu.gemini.tac.qengine.p1.Mode
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.core.Semester

class ObservtionIdTest {

  @Test def testNegativeObservationIndicesNotAllowed() {
    val progId = ProgramId(Site.GN, Semester.parse("2011A"), Mode.Queue, 1)
    try {
      ObservationId(progId, -1)
      fail()
    } catch {
      case ex: IllegalArgumentException => // ok
    }
  }

  private def assertLessThan(o1: ObservationId, o2: ObservationId) {
    assertTrue(o1 < o2)
    assertFalse(o1 == o2)
    assertFalse(o2 < o1)
  }

  @Test def testProgramOrderingFirst() {
    val p1 = ProgramId(Site.GN, Semester.parse("2011B"), Mode.Queue, 1)
    val p2 = ProgramId(Site.GN, Semester.parse("2011B"), Mode.Queue, 2)

    val o1 = ObservationId(p1, 99)
    val o2 = ObservationId(p2, 1)
    assertLessThan(o1, o2)
  }

  @Test def testObservationIndexOrderingSecond() {
    val p = ProgramId(Site.GN, Semester.parse("2011B"), Mode.Queue, 1)

    val o1 = ObservationId(p, 1)
    val o2 = ObservationId(p, 2)
    assertLessThan(o1, o2)
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
      "GN-2011A-Q-8",
      "GN-2011A-Q-8--1",
      "GN-2011A-Q-8-1-1"
    )

    nones.foreach(n => assertEquals(None, ObservationId.parse(n)))
  }

  @Test def testSomeStringsThatParse() {
    val progId = ProgramId(Site.GN, Semester.parse("2011A"), Mode.Queue, 1)
    val ids = for {
      index <- 1 to 10
    } yield ObservationId(progId, index)

    ids.foreach { id =>
      assertEquals(Some(id), ObservationId.parse(id.toString))
    }
  }

  @Test def testDelegatesToProgId() {
    val semester = Semester.parse("2011A")
    val progId   = ProgramId(Site.GN, semester, Mode.Queue, 42)
    val obsId    = ObservationId(progId, 99)

    assertEquals(Site.GN, obsId.site)
    assertEquals(semester, obsId.semester)
    assertEquals(Mode.Queue, obsId.mode)
    assertEquals(42, obsId.progIndex)
    assertEquals(99, obsId.index)
  }
}
