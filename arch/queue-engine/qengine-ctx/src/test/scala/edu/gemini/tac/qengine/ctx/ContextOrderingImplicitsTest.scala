package edu.gemini.tac.qengine.ctx

import org.junit._
import Assert._

class ContextOrderingImplicitsTest {
  import ContextOrderingImplicits._

  @Test def testSiteOrdering() {
    val s1 = Site.north
    val s2 = Site.south

    val orderedList = List(s1, s2)
    assertEquals(orderedList, orderedList.sorted)

    val unorderedList = List(s2, s1)
    assertEquals(orderedList, unorderedList.sorted)
  }

  @Test def testSemesterOrdering() {
    val s1 = Semester.parse("2010B")
    val s2 = Semester.parse("2011A")
    val s3 = Semester.parse("2010B")

    val orderedList = List(s1, s3, s2)
    assertEquals(orderedList, orderedList.sorted)

    val unorderedList = List(s2, s3, s1)
    assertEquals(orderedList, unorderedList.sorted)

    // Rich Semester
    assertTrue(s1 < s2)
    assertTrue(s2 > s3)
    assertEquals(s2, s1 max s2 max s3)
  }

  @Test def testContextOrdering() {
    val s10B = Semester.parse("2010B")
    val s11A = Semester.parse("2011A")
    val gn   = Site.north
    val gs   = Site.south

    val gn_10B = new Context(gn, s10B)
    val gn_11A = new Context(gn, s11A)
    val gs_10B = new Context(gs, s10B)
    val gs_11A = new Context(gs, s11A)

    val orderedList = List(gn_10B, gs_10B, gn_11A, gs_11A)
    assertEquals(orderedList, orderedList.sorted)

    val unorderedList = List(gs_11A, gn_10B, gs_10B, gn_11A)
    assertEquals(orderedList, unorderedList.sorted)
  }
}