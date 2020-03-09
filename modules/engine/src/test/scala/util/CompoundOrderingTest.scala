package edu.gemini.tac.qengine.util

import org.junit._
import Assert._

/**
 *
 */
class CompoundOrderingTest {
  case class Obj(i: Int, s: String, d: Double)

  object IntOrdering extends Ordering[Obj] {
    def compare(o1: Obj, o2: Obj) = o1.i.compare(o2.i)
  }
  object StringOrdering extends Ordering[Obj] {
    def compare(o1: Obj, o2: Obj) = o1.s.compare(o2.s)
  }
  object DoubleOrdering extends Ordering[Obj] {
    def compare(o1: Obj, o2: Obj) = o1.d.compare(o2.d)
  }

  @Test def testNone() {
    val c = new CompoundOrdering[Obj]()

    // Anything goes.
    val o1 = Obj(1, "one", 1.0)
    val o2 = Obj(2, "two", 2.0)
    val o3 = Obj(3, "three", 3.0)

    // Just preserve the ordering.
    val lst = List(o1, o2, o3)
    assertEquals(lst, lst.sorted(c))
  }

  @Test def testOne() {
    val c = new CompoundOrdering(StringOrdering)

    val o1 = Obj(1, "one", 1.0)
    val o2 = Obj(2, "two", 2.0)
    val o3 = Obj(3, "three", 3.0)
    val o4 = Obj(4, "four", 4.0)

    val lst = List(o1, o2, o3, o4)
    val exp = List(o4, o1, o3, o2)
    assertEquals(exp, lst.sorted(c))
  }

  @Test def testTwo() {
    val c = new CompoundOrdering(StringOrdering, IntOrdering)

    val o1 = Obj(1, "a", 7.0)
    val o2 = Obj(2, "b", 6.0)
    val o3 = Obj(3, "b", 5.0)
    val o4 = Obj(4, "a", 4.0)

    val lst = List(o1, o2, o3, o4)
    val exp = List(o1, o4, o2, o3)
    assertEquals(exp, lst.sorted(c))
  }

  @Test def testThree() {
    val c = new CompoundOrdering(StringOrdering, IntOrdering, DoubleOrdering)

    val o1 = Obj(1, "a", 7.0)
    val o2 = Obj(2, "b", 6.0)
    val o3 = Obj(3, "b", 5.0)
    val o4 = Obj(4, "a", 4.0)
    val o5 = Obj(2, "b", 3.0)
    val o6 = Obj(2, "b", 2.0)
    val o7 = Obj(4, "a", 1.0)

    val lst = List(o1, o2, o3, o4, o5, o6, o7)
    val exp = List(o1, o7, o4, o6, o5, o2, o3)
    assertEquals(exp, lst.sorted(c))
  }
}
