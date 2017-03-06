package edu.gemini.qengine.skycalc

import org.junit._
import Assert._

class HoursTest {

  @Test def testFromMillisec() {
    val expected = new Hours(42.0)
    assertEquals(new Hours(42), Hours.fromMillisec(42l * 60 * 60 * 1000))
  }
}