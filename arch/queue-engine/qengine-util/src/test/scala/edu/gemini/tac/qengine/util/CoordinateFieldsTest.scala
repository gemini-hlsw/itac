package edu.gemini.tac.qengine.util

import CoordinateFields.Sign
import Sign._

import org.junit.Test
import org.junit.Assert._

/**
 * Coordinate parsing code tests.
 */
class CoordinateFieldsTest {
  @Test def testNull() {
    assertEquals(None, Sign.parse(null))
    assertEquals(None, CoordinateFields.parse(null))
  }

  @Test def testSignParse() {
    import Sign.{parse => p}
    assertEquals(Some(Pos), p("+"))
    assertEquals(Some(Neg), p("-"))
    assertEquals(None,      p("x"))
    assertEquals(None,      p(""))
  }

  @Test def testNoMatch() {
    val nones = List(
        "xyz"
      , "*10:20:30"
      , ""
      , "10.0:20:30"
      , "10::20:30"
    )
    nones.foreach(n => assertTrue(CoordinateFields.parse(n).isEmpty))
  }

  private def verify1020304(s: String) {
    CoordinateFields.parse(s) match {
      case Some(CoordinateFields(Pos, 10, 20, 30.4)) => //ok
      case _ => fail
    }
  }

  @Test def testParse() {
    val somes = List(
        "10:20:30.4"
      , " 10:20:30.4 "
      , "+10:20:30.4"
      , " + 10  20\t:\t30.4   "
      , "10 20 30.4"
      , "10:20 30.4"
      , "10\t20\t30.4"
      , "10:20:30.4000"
    )
    somes.foreach(verify1020304)
  }

  @Test def testNoSecNeg() {
    val somes = List(
        "-10:20"
      , "-10 20"
      , "-10 20.0"
      , " -10:20"
      , "-10:20 "
      , " -10:20 "
    )
    somes.foreach {
      s => assertEquals(CoordinateFields(Neg, 10, 20, 0.0), CoordinateFields.parse(s).get)
    }
  }

  @Test def testNoSecPos() {
    val somes = List(
        "10:20"
      , "+10:20"
      , "10 20"
      , "+10 20"
      , "10 20.0"
      , "+10 20.0"
      , " 10:20"
      , " +10:20"
      , "10:20 "
      , "+10:20 "
      , " 10:20 "
      , " +10 20 "
    )
    somes.foreach {
      s => assertEquals(CoordinateFields(Pos, 10, 20, 0.0), CoordinateFields.parse(s).get)
    }
  }

  @Test def testNoSecDecimal() {
    CoordinateFields.parse("-10 30.5") match {
      case Some(CoordinateFields(Neg, 10, 30, 30.0)) => // ok
      case _ => fail
    }
  }

  @Test def testNoSecWeirdMinutes() {
    assertEquals(-11.5083333, CoordinateFields.parse("-10 90.5").get.asHrs.mag, 0.000001)
  }

  @Test def testNoMinNeg() {
    val somes = List(
        "-10"
      , " -10"
      , " -10 "
      , "-10 "
    )
    somes.foreach {
      s => assertEquals(CoordinateFields(Neg, 10, 0, 0.0), CoordinateFields.parse(s).get)
    }
  }

  @Test def testNoMinPos() {
    val somes = List(
        "10"
      , "+10"
      , " 10"
      , " +10"
      , " 10 "
      , " +10 "
      , "10 "
      , "+10 "
    )
    somes.foreach {
      s => assertEquals(CoordinateFields(Pos, 10, 0, 0.0), CoordinateFields.parse(s).get)
    }
  }

  @Test def testNoMinDecimal() {
    val deg = CoordinateFields.parse("10.76").get.asDeg.mag
    assertEquals(10.76, deg, 0.0000001)
  }

  @Test def testNeg() {
    CoordinateFields.parse("-10:00:00") match {
      case Some(CoordinateFields(Neg, 10, 0, 0.0)) => // ok
      case _ => fail
    }
  }

  @Test def testSingleDigits() {
    CoordinateFields.parse("1:2:3.4") match {
      case Some(CoordinateFields(Pos, 1, 2, 3.4)) => // ok
      case _ => fail
    }
  }

  @Test def testAs() {
    import CoordinateFields.{parse => p}
    assertEquals(150.0,   p("10:00:00").get.asHrs.toDeg.mag, 0.0000001)
    assertEquals( 10.0,   p("10:00:00").get.asDeg.mag, 0.0000001)
    assertEquals(210.0,   p("-10:00:00").get.asHrs.toDeg.toPositive.mag, 0.0000001)
    assertEquals(-10.0,   p("-10:00:00").get.asDeg.mag, 0.0000001)
    assertEquals(152.542, p("10:10:10.10").get.asHrs.toDeg.mag, 0.0001)
  }

  @Test def testWrap() {
    import CoordinateFields.{parse => p}
    assertEquals(1.0, p("25.0").get.asHrs.mag, 0.0000001)
  }

  @Test def testTrailingDecimal() {
    import CoordinateFields.{parse => p}
    assertEquals(150.0, p("10:00:00.").get.asHrs.toDeg.mag, 0.0000001)
  }
}