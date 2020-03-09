package edu.gemini.tac.qengine.util

import Angle._
import org.junit._
import Assert._

class AngleTest {
  val prec = 0.000001

  @Test def testConstruction() {
    // Normal construction
    var a = new Angle(180.0, Deg)
    assertEquals(180.0, a.mag, prec)
    assertEquals(Deg, a.unit)

    // Using a multiple of a circle.
    a = new Angle(361.0, Deg)
    assertEquals(1.0, a.mag, prec)

    a = new Angle(360.5, Deg)
    assertEquals(0.5, a.mag, prec)

    a = new Angle(-360.5, Deg)
    assertEquals(-0.5, a.mag, prec)

    a = new Angle(25.0, Hr)
    assertEquals(1.0, a.mag, prec)
  }

  @Test def testPositiveAngle() {
    val a = new Angle(-1.0, Hr)
    val b = a.toPositive
    assertEquals(23.0, b.mag, prec)
    assertNotSame(a, b)
    assertTrue(a.equals(b))
    assertSame(b, b.toPositive)
  }

  @Test def testNegativeAngle() {
    val a = new Angle(1.0, Hr)
    val b = a.toNegative
    assertEquals(-23.0, b.mag, prec)
    assertNotSame(a, b);
    assertTrue(a.equals(b))
    assertSame(b, b.toNegative)
  }

  val a180 = List(
    new Angle(180 * 60 * 60 * 1000, Mas),
    new Angle(180 * 60 * 60, Arcsec),
    new Angle(180 * 60, Arcmin),
    new Angle(180, Deg),
    new Angle(12 * 60 * 60, Sec),
    new Angle(12 * 60, Min),
    new Angle(12, Hr),
    new Angle(math.Pi, Rad)
  )

  @Test def testConversion() {
    for (a <- a180; u <- Angle.allUnits) {
      val b = a.convertTo(u)
      if (a.unit == u) assertSame(a, b)
      else assertEquals(b, a)
    }
  }

  // Normal comparisons among the same units.
  @Test def testCompareAngle_Normal() {
    val a0 = new Angle(0, Deg)
    val a1 = new Angle(1, Deg)

    assertEquals(-1, a0.compare(a1))
    assertEquals(1, a1.compare(a0))
    assertEquals(0, a0.compare(new Angle(0, Deg)))
  }

  // Mixed unit comparisons.
  @Test def testCompareAngle_Mixed() {
    val a59s = new Angle(59, Arcsec);
    val a60s = new Angle(60, Arcsec);
    val a61s = new Angle(61, Arcsec);
    val a1m  = new Angle(1, Arcmin);

    assertEquals(-1, a59s.compare(a1m))
    assertEquals(0, a60s.compare(a1m))
    assertEquals(1, a61s.compare(a1m))

    assertEquals(-1, a1m.compare(a61s))
    assertEquals(0, a1m.compare(a60s))
    assertEquals(1, a1m.compare(a59s))
  }

  // +/- angle comparisons
  @Test def testCompareAngle_Negative() {
    val a_9  = new Angle(-9, Deg)
    val a_10 = new Angle(-10, Deg)
    val a_11 = new Angle(-11, Deg)
    val a350 = new Angle(350, Deg)

    assertEquals(1, a_9.compare(a_10))
    assertEquals(0, a_10.compare(a_10))
    assertEquals(-1, a_11.compare(a_10))
    assertEquals(0, a350.compare(a_10))
    assertEquals(0, a_10.compare(a350))
  }

  @Test def testAdd() {
    assertEquals(Angle.anglePiOver2, Angle.angle2Pi + Angle.anglePiOver2)
    assertEquals(Angle.anglePiOver2, Angle.angle2Pi.add(math.Pi / 2, Rad))
    assertEquals(Angle.anglePiOver2, Angle.angle2Pi.add(-3 * math.Pi / 2, Rad))

    assertEquals(Angle.anglePiOver2, Angle.anglePiOver2 + Angle.angle2Pi)
    assertEquals(Angle.anglePiOver2, Angle.anglePiOver2.add(2 * math.Pi, Rad))
    assertEquals(Angle.anglePiOver2, Angle.anglePiOver2.add(-2 * math.Pi, Rad))

    assertEquals(Angle.anglePiOver2, Angle.anglePi + Angle.angle3PiOver2)
    assertEquals(Angle.anglePiOver2, Angle.anglePi.add(3 * math.Pi / 2, Rad))
    assertEquals(Angle.anglePiOver2, Angle.anglePi.add(-math.Pi / 2, Rad))

    assertEquals(Angle.anglePiOver2, Angle.angle3PiOver2 + Angle.anglePi)
    assertEquals(Angle.anglePiOver2, Angle.angle3PiOver2.add(math.Pi, Rad))
    assertEquals(Angle.anglePiOver2, Angle.angle3PiOver2.add(-math.Pi, Rad))
  }

  @Test def testHash() {
    var a = new Angle(0, Deg)
    assertEquals(0.0.hashCode, a.hashCode)

    a = new Angle(-90.0, Deg)
    assertEquals(270.0.hashCode, a.hashCode)

    a = new Angle(12.0, Hr)
    assertEquals(180.0.hashCode, a.hashCode)
  }
}
