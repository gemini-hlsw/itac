package edu.gemini.tac.qengine.ctx

import org.junit._
import Assert._

/**
 * Test cases for Site.
 */
class SiteTest {
  @Test def testSomeNorth() {
    val norths = List("Gemini North", "GEMINI NORTH", "NoRtH", "xyzNORTHxyz", "GN", "Mauna Kea", "MKO", "HBF", "Hilo")
    norths.foreach {
      n => Site.parse(n) match {
        case Site.north => // ok
        case _ => fail("Expected Gemini North.  Couldn't parse " + n)
      }
    }
  }

  @Test def testSomeSouth() {
    val souths = List("Gemini South", "GEMINI SOUTH", "SoUtH", "xyzSOUTHxyz", "GS", "Cerro Pachon", "CPO", "SBF", "La Serena")
    souths.foreach {
      s => Site.parse(s) match {
        case Site.south => // ok
        case _ => fail("Expected Gemini South.  Couldn't parse " + s)
      }
    }
  }

  @Test def testNone() {
    val nones = List("", "Gemini", "(XYZ)")
    nones.foreach {
      n => Site.parse(n) match {
        case null => // ok
        case _ => fail("Expected None. Incorrectly parsed: " + n);
      }
    }
  }

}