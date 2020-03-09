package edu.gemini.tac.qengine.util

import org.junit._
import Assert._

final class PercentTest {
  @Test def testInt() {
    assertEquals(10, Percent(5) * 200, Double.MinPositiveValue)
  }

  @Test def testLong() {
    assertEquals(10L, Percent(5) * 200L, Double.MinPositiveValue)
  }

  @Test def testDouble() {
    assertEquals(10.0, Percent(5) * 200.0, Double.MinPositiveValue)
  }

  /*
  @Test def testImplicitInt() {
    assertEquals(10, 200 * Percent(5))
  }

  @Test def testImplicitLong() {
    assertEquals(10l, 200l * Percent(5))
  }

  @Test def testImplicitDouble() {
    assertEquals(10.0, 200.0 * Percent(5), 0.000001)
  }
   */

  @Test def testSumPercents() {
    assertEquals(Percent(42), Percent(2) + Percent(40))
  }

  @Test def testSubtractPercents() {
    assertEquals(Percent(42), Percent(44) - Percent(2))
  }

  private def testRelativePercentages[T](precision: Int, in: List[T], exp: Double*)(
    implicit num: Numeric[T]
  ): Unit = {
    assertEquals(exp.toList.map(Percent(_, precision)), Percent.relativePercentages(in, precision))
  }

  @Test def testRelativePercentages() {
    val threes = List(3, 3, 3)
    testRelativePercentages(0, threes, 34, 33, 33)
    testRelativePercentages(1, threes, 33.4, 33.3, 33.3)
    testRelativePercentages(2, threes, 33.34, 33.33, 33.33)
    testRelativePercentages(3, threes, 33.334, 33.333, 33.333)

    testRelativePercentages(2, List(25.0, 75.0), 25.0, 75.0)
    testRelativePercentages(2, List(25.0, 50.0), 33.33, 66.67)
    testRelativePercentages(2, List(7), 100.0)
    testRelativePercentages(2, List(0, 1, 1), 0, 50, 50)
    testRelativePercentages(2, List(-2, 12), -20, 120)
  }
}
