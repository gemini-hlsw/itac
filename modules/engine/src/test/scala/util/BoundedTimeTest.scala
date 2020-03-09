package edu.gemini.tac.qengine.util

import org.junit._
import Assert._

class BoundedTimeTest {
  val Delta = 0.000001

  @Test def testConstruct() {
    try {
      BoundedTime(Time.hours(-0.1))
      fail
    } catch {
      case ex: Exception => // ok
    }

    try {
      BoundedTime(Time.hours(0.1), Time.hours(-0.1))
      fail
    } catch {
      case ex: Exception => // ok
    }
  }

  @Test def testEmpty() {
    val t  = Time.hours(1.0)
    val bt = BoundedTime(t)
    assertTrue(bt.isEmpty)
    assertFalse(bt.isFull)
    assertEquals(t, bt.remaining)
    assertEquals(0.0, bt.fillPercent, Delta)

    assertEquals(bt, bt.empty)

    assertSame(bt, bt.empty)
    assertEquals(BoundedTime(t, t), bt.fill)
    assertEquals(BoundedTime(t, t), bt.fillExact)
  }

  @Test def testZeroLimit() {
    val t  = Time.Zero
    val bt = BoundedTime(t)

    assertTrue(bt.isEmpty)
    assertTrue(bt.isFull)
    assertFalse(bt.isOverbooked)
    assertEquals(Time.Zero, bt.remaining)
    assertEquals(100.0, bt.fillPercent, Delta)

    assertSame(bt, bt.empty)
    assertSame(bt, bt.fill)
    assertSame(bt, bt.fillExact)
  }

  @Test def testFull() {
    val t  = Time.hours(1.0)
    val bt = BoundedTime(t, t)
    assertFalse(bt.isEmpty)
    assertTrue(bt.isFull)
    assertFalse(bt.isOverbooked)
    assertEquals(Time.Zero, bt.remaining)
    assertEquals(100.0, bt.fillPercent, Delta)

    assertEquals(bt, bt.fill)
  }

  @Test def testMid() {
    val l  = Time.hours(10.0)
    val u  = Time.hours(6.0)
    val r  = Time.hours(4.0)
    val bt = BoundedTime(l, u)

    assertFalse(bt.isEmpty)
    assertFalse(bt.isFull)
    assertFalse(bt.isOverbooked)
    assertEquals(r.value, bt.remaining.value, Delta)
    assertEquals(60.0, bt.fillPercent, Delta)

    assertEquals(BoundedTime(l), bt.empty)
    assertEquals(BoundedTime(l, l), bt.fill)
  }

  @Test def testOver() {
    val l  = Time.hours(10.0)
    val u  = Time.hours(15.0)
    val r  = Time.hours(-5.0)
    val bt = BoundedTime(l, u)

    assertFalse(bt.isEmpty)
    assertTrue(bt.isFull)
    assertTrue(bt.isOverbooked)
    assertEquals(r.value, bt.remaining.value, Delta)
    assertEquals(150.0, bt.fillPercent, Delta)

    assertEquals(BoundedTime(l), bt.empty)
    assertSame(bt, bt.fill)
    assertEquals(BoundedTime(l, l), bt.fillExact)
  }

  @Test def testMixUnitPercentage() {
    val l  = Time.hours(1.0)
    val u  = Time.minutes(15.0)
    val r  = Time.hours(0.75)
    val bt = BoundedTime(l, u)

    assertEquals(25.0, bt.fillPercent, Delta)
  }

  @Test def testReserveNormal() {
    val bt1 = BoundedTime(Time.hours(1.0))
    val bt2 = bt1.reserve(Time.minutes(59)).get
    val bt3 = bt2.reserve(Time.minutes(1)).get
    assertEquals(None, bt3.reserve(Time.minutes(1)))
  }

  @Test def testReserveOverbooked() {
    val bt = BoundedTime(Time.hours(1.0), Time.hours(2.0))
    assertEquals(None, bt.reserve(Time.seconds(1.0)))
  }

  @Test def testReserveNegative() {
    val bt1 = BoundedTime(Time.hours(1.0), Time.minutes(30.0))
    val bt2 = bt1.reserve(Time.minutes(-10.0)).get

    assertEquals(Time.minutes(40.0), bt2.remaining)
  }

  @Test def testUnderReserve() {
    val bt1 = BoundedTime(Time.hours(1.0), Time.minutes(30.0))
    bt1.reserve(Time.minutes(-40.0)) match {
      case None => // ok
      case _    => fail
    }
  }

  @Test def testOverbook() {
    val bt1 = BoundedTime(Time.hours(1.0))
    val bt2 = bt1.overbook(Time.minutes(59)).get
    val bt3 = bt2.overbook(Time.minutes(1)).get
    bt3.overbook(Time.minutes(1.0)) match {
      case Some(BoundedTime(limit, used)) =>
        assertEquals(61.0, used.toMinutes.value, Delta)
        assertEquals(60.0, limit.toMinutes.value, Delta)
      case _ => fail
    }

    bt3.overbook(-Time.minutes(61.0)) match {
      case None => // ok
      case _    => fail
    }
  }

  @Test def testReserveAvailable() {
    val bt1           = BoundedTime(Time.hours(1.0))
    val (bt2, spill2) = bt1.reserveAvailable(Time.minutes(59))
    assertEquals(Time.Minutes.zero, spill2)
    assertFalse(bt2.isFull)

    val (bt3, spill3) = bt2.reserveAvailable(Time.minutes(2))
    assertEquals(Time.minutes(1), spill3)
    assertTrue(bt3.isFull)

    val t10           = Time.minutes(10)
    val (bt4, spill4) = bt3.reserveAvailable(t10)
    assertSame(bt3, bt4)
    assertEquals(t10, spill4)
  }

  @Test def testReleaseAvailable() {
    val bt1           = BoundedTime(Time.hours(1.0), Time.hours(1.0))
    val (bt2, spill2) = bt1.releaseAvailable(Time.minutes(59))
    assertEquals(Time.Minutes.zero, spill2)
    assertEquals(Time.minutes(1.0).value, bt2.used.toMinutes.value, Delta)

    val (bt3, spill3) = bt2.releaseAvailable(Time.minutes(2))
    assertEquals(-Time.minutes(1.0).value, spill3.toMinutes.value, Delta)
    assertTrue(bt3.isEmpty)
  }
}
