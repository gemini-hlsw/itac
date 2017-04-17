package edu.gemini.tac.qengine.api.config

import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.Percent
import edu.gemini.tac.qengine.p1.QueueBand._

class QueueBandPercentagesTest {

  private def verify(percs: QueueBandPercentages, b1: Int, b2: Int, b3: Int) {
    assertEquals(Percent(b1), percs.band1)
    assertEquals(Percent(b1), percs(QBand1))

    assertEquals(Percent(b2), percs.band2)
    assertEquals(Percent(b2), percs(QBand2))

    assertEquals(Percent(b3), percs.band3)
    assertEquals(Percent(b3), percs(QBand3))
  }

  @Test def testDefault() {
    verify(QueueBandPercentages.Default, 30, 30, 20)
  }

  @Test def testNonDefault() {
    verify(QueueBandPercentages(Percent(10), Percent(20), Default.Band3Percent), 10, 20, 20)
  }

  @Test def testNonDefaultInt() {
    verify(QueueBandPercentages(10, 20, 30), 10, 20, 30)
  }

  @Test def testNegativePercent() {
    try {
      QueueBandPercentages(-10, 0, 0)
    } catch {
      case ex: IllegalArgumentException => // ok
    }

    try {
      QueueBandPercentages(10, -20, 0)
    } catch {
      case ex: IllegalArgumentException => // ok
    }
  }

  @Test def testOver100Percent() {
    try {
      QueueBandPercentages(10, 110, 0)
    } catch {
      case ex: IllegalArgumentException => // ok
    }

    try {
      QueueBandPercentages(110, 120, 0)
    } catch {
      case ex: IllegalArgumentException => // ok
    }
  }

  @Test def testBadSum() {
    try {
      QueueBandPercentages(50, 50, 50)
    } catch {
      case ex: IllegalArgumentException => // ok
    }
  }

  @Test def testToString() {
    val percs0 = QueueBandPercentages.Default
    assertEquals("(B1=30%, B2=30%, B3=20%)", percs0.toString)

    val percs1 = QueueBandPercentages(10, 20, 40)
    assertEquals("(B1=10%, B2=20%, B3=40%)", percs1.toString)
  }

  @Test def testCategoryPercent() {
    val qbp = QueueBandPercentages(5,15,25)

    val tests = List(
      (20, Category.B1_2),
      (25, Category.B3),
      (45, Category.Guaranteed),
      (55, Category.PoorWeather)
    )

    tests foreach { case (n, cat) => assertEquals(Percent(n), qbp(cat)) }
  }
}