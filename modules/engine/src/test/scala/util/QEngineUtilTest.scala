package edu.gemini.tac.qengine.util

import org.junit._
import Assert._

class QEngineUtilTest {

  @Test def testToListFromNull() {
    QEngineUtil.toList(null) match {
      case Nil => // ok
      case _   => fail
    }
  }

  @Test def testToListNotNull() {
    val jlist = new java.util.ArrayList[Int]()
    jlist.add(42)
    jlist.add(99)

    QEngineUtil.toList(jlist) match {
      case List(42, 99) => // ok
      case _            => fail
    }
  }

  @Test def testPromoteEitherLeft() {
    val elist = List[Either[String, Int]](Right(42), Left("bad"), Right(99))

    QEngineUtil.promoteEither(elist) match {
      case Left("bad") => // ok
      case _           => fail
    }
  }

  @Test def testPromoteEitherRight() {
    val elist = List[Either[String, Int]](Right(42), Right(2), Right(99))

    QEngineUtil.promoteEither(elist) match {
      case Right(List(42, 2, 99)) => // ok
      case _                      => fail
    }
  }

  @Test def testPromoteEitherNil() {
    QEngineUtil.promoteEither(Nil) match {
      case Right(Nil) => //ok
      case _          => fail
    }
  }

  @Test def testTrimString() {
    import QEngineUtil.{trimString => f}

    val nones = List[String](
      null,
      "",
      " ",
      " \t "
    )

    nones.foreach { n =>
      f(n) match {
        case None    => // ok
        case Some(s) => fail(s)
      }
    }

    val his = List(
      "hi",
      " hi",
      "\thi\t"
    )

    his.foreach { n =>
      f(n) match {
        case Some("hi") => // ok
        case _          => fail
      }
    }
  }
}
