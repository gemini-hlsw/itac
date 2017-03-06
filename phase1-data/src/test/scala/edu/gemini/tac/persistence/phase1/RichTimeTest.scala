package edu.gemini.tac.persistence.phase1

import org.junit._

import RichTime.toRichTime
import java.math.BigDecimal
import org.junit.Assert._

class RichTimeAmountTest {

  // t1 == t3, t1 < t2
  private def verifyOrder(t1: TimeAmount, t2: TimeAmount, t3: TimeAmount) {
    assertTrue(t1 ltTime t2)
    assertTrue(t1 leTime t2)

    assertTrue(t1 leTime t3)
    assertTrue(t1 eqTime t3)
    assertTrue(t1 geTime t3)

    assertTrue(t2 gtTime t1)
    assertTrue(t2 geTime t1)
  }

  @Test def testSameUnits() {
    val t1 = new TimeAmount(1.0, TimeUnit.HR)
    val t2 = new TimeAmount(2.0, TimeUnit.HR)
    val t3 = new TimeAmount(1.0, TimeUnit.HR)
    verifyOrder(t1, t2, t3)
  }

  @Test def testDifferentUnits() {
    val t1 = new TimeAmount(60.0, TimeUnit.MIN)
    val t2 = new TimeAmount(1.1, TimeUnit.HR)
    val t3 = new TimeAmount(1.0, TimeUnit.HR)
    verifyOrder(t1, t2, t3)
  }

  @Test def testSumTimeAmountsWithDiffereningUnits() {
    val t1 = new TimeAmount(1.0, TimeUnit.MIN)
    val t2 = new TimeAmount(1.0, TimeUnit.HR)

    val t3 = t1 + t2
    val t4 = t2 + t1

    assertEquals(0, t3.getValue.compareTo(new java.math.BigDecimal("61.0")))
    assertEquals(0, t4.getValue.compareTo(new BigDecimal("61.0")))
    assertEquals(TimeUnit.MIN, t3.getUnits)
    assertEquals(TimeUnit.MIN, t4.getUnits)
  }

  @Test def testSumTimeAmountsWithSameUnits() {
    val t1 = new TimeAmount(60.0, TimeUnit.MIN)
    val t2 = new TimeAmount(1.0, TimeUnit.MIN)

    val t3 = t1 + t2
    val t4 = t2 + t1

    assertEquals(0, t3.getValue.compareTo(new java.math.BigDecimal("61.0")))
    assertEquals(0, t4.getValue.compareTo(new java.math.BigDecimal("61.0")))
    assertEquals(TimeUnit.MIN, t3.getUnits)
    assertEquals(TimeUnit.MIN, t4.getUnits)
  }

  @Test def testSubtractTimesWithDiffereningUnits() {
    val t1 = new TimeAmount(1.0, TimeUnit.MIN)
    val t2 = new TimeAmount(1.0, TimeUnit.HR)

    val t3 = t1 - t2
    val t4 = t2 - t1

    assertEquals(0, new java.math.BigDecimal("-59.0").compareTo(t3.getValue))
    assertEquals(0, new java.math.BigDecimal("59.0").compareTo(t4.getValue))
    assertEquals(TimeUnit.MIN, t3.getUnits)
    assertEquals(TimeUnit.MIN, t4.getUnits)
  }

  @Test def testSubtractTimesWithSameUnits() {
    val t1 = new TimeAmount(60.0, TimeUnit.MIN)
    val t2 = new TimeAmount(1.0, TimeUnit.MIN)

    val t3 = t1 - t2
    val t4 = t2 - t1

    assertEquals(0, new java.math.BigDecimal("59.0").compareTo(t3.getValue))
    assertEquals(0, new java.math.BigDecimal("-59.0").compareTo(t4.getValue))
    assertEquals(TimeUnit.MIN, t3.getUnits)
    assertEquals(TimeUnit.MIN, t4.getUnits)
  }

  @Test def testSumList() {

    val t1 = new TimeAmount(60.0, TimeUnit.MIN)
    val t2 = new TimeAmount(1.0, TimeUnit.MIN)

    val Some(t3) = RichTime.sum(List(t1, t2))
    assertEquals(0, new java.math.BigDecimal("61.0").compareTo(t3.getValue))
    assertEquals(TimeUnit.MIN, t3.getUnits)
  }

  @Test def testSumEmptyList() {
    RichTime.sum(Nil) match {
      case None => // ok
      case _ => fail()
    }
  }

  @Test def testCanCompareTimes() {
    val t1 = new TimeAmount(60.0, TimeUnit.MIN)
    val t2 = new TimeAmount(60.0, TimeUnit.MIN)
    val t3 = new TimeAmount(1.0, TimeUnit.HR)
    val t4 = new TimeAmount(2.0, TimeUnit.HR)

    assertEquals(0, t1.compareTo(t2))
    assertEquals(0, t1.compareTo(t3))
    assertEquals(-1, t1.compareTo(t4))
    assertEquals(1, t4.compareTo(t1))
  }
}