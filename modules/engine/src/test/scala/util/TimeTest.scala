package edu.gemini.tac.qengine.util

import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.Time.toPercentMultiplier

class TimeTest {
  @Test def testPercent() {
    val t            = Time.hours(1.0)
    val fiftyPercent = Percent(50)
    assertEquals(Time.hours(0.5), t.percent(50))
    assertEquals(Time.hours(0.5), t * fiftyPercent)
    assertEquals(Time.hours(0.5), fiftyPercent * t)

    // 100%
    assertSame(t, t * Percent(100))
    assertSame(t, t.percent(100))
    assertSame(t, Percent(100) * t)

    // 0%
    assertEquals(Time.Zero, t.percent(0))
    assertEquals(Time.Zero, t * Percent(0))
    assertEquals(Time.Zero, Percent(0) * t)

    // 60%
    assertEquals(Time.hours(0.6), t.percent(60))

    // No overflow ...
    assertEquals(Time.nights(300.0), Time.nights(1000.0).percent(30))
  }

  @Test def testUnary_-() {
    val h = Time.hours(1.0)
    assertEquals(h, -(-h))
    assertEquals(-1.0, -h.value, 0.000001)

    val h2 = Time.hours(2.0)
    assertEquals(h.value, (h2 + -h).value, 0.000001)
  }

  @Test def testEquals() {
    val h = Time.hours(1.0)
    val m = Time.minutes(60.0)
    assertEquals(h, m)
    assertEquals(h.hashCode, m.hashCode)
    assertTrue(h == m)
  }

  @Test def testOrdered() {
    val m59 = Time.minutes(59.0)
    val h1  = Time.hours(1.0)
    val m60 = Time.minutes(60.0)
    val m61 = Time.minutes(61.0)

    assertTrue(m59 < h1)
    assertTrue(m59 < m60)

    assertTrue(h1 == m60)
    assertFalse(h1 < m60)
    assertFalse(m60 < h1)

    assertEquals(List(m59, m60, h1, m61), List(m60, m59, h1, m61).sorted)
  }

  @Test def add() {
    assertEquals(Time.hours(1.0), Time.minutes(30.0) + Time.hours(0.5))
  }

  @Test def sub() {
    assertEquals(Time.minutes(15.0), Time.minutes(30.0) - Time.hours(0.25))
  }

  @Test def negative() {
    assertEquals(Time.minutes(-5.0), Time.minutes(30.0) - Time.minutes(35.0))
  }

}
