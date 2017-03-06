package edu.gemini.tac.qservice.impl.queue

import org.junit._
import Assert._

import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.{Committee => PsCommittee}
import edu.gemini.tac.persistence.restrictedbin.{LgsObservationsRestrictedBin      => PsLgsObservationRestrictedBin}
import edu.gemini.tac.persistence.restrictedbin.{RestrictedBin                     => PsRestrictedBin}
import edu.gemini.tac.persistence.restrictedbin.{WaterVaporPercentageRestrictedBin => PsWaterVaporRestrictedBin}
import edu.gemini.tac.psconversion.BadData
import edu.gemini.tac.qengine.util.{Time, Percent}

import scala.collection.JavaConversions._

import scalaz._

class TimeRestrictionExtractorTest {
  private def mkQueue(r: PsRestrictedBin*): PsQueue = {
    val s: Set[PsRestrictedBin] = Set(r: _*)
    val c = new PsCommittee
    val q = new PsQueue("Test Queue Name", c)
    q.setRestrictedBins(s) // via JavaConversions
    q
  }

  @Test def testNone() {
    val e = TimeRestrictionExtractor.extract(mkQueue())
    e match {
      case \/-((Nil, Nil)) => // ok
      case _ => fail()
    }
  }

  private def mkWvBin(value: Int): PsWaterVaporRestrictedBin = {
    new PsWaterVaporRestrictedBin {
      override def getValue = new java.lang.Integer(value)
    }
  }

  private def mkLgsBin(value: Int): PsLgsObservationRestrictedBin = {
    new PsLgsObservationRestrictedBin {
      override def getValue = new java.lang.Integer(value)
    }
  }

  @Test def testRelative() {
    val bin = mkWvBin(42)
    val e = TimeRestrictionExtractor.extract(mkQueue(bin))
    e match {
      case \/-((List(wv), Nil)) => {
        assertEquals(Percent(42), wv.value)
      }
      case _ => fail()
    }
  }

  private def verifyBadValue(bin: PsRestrictedBin, msg: String) {
    val e = TimeRestrictionExtractor.extract(mkQueue(bin))
    e match {
      case -\/(BadData(error)) => assertEquals(msg, error)
      case _ => fail()
    }
  }

  @Test def testHighPercentage() {
    val bin = mkWvBin(110)
    val msg = TimeRestrictionExtractor.BAD_PERCENT_VALUE(Percent(110))
    verifyBadValue(bin, msg)
  }

  @Test def testLowPercentage() {
    val bin = mkWvBin(-1)
    val msg = TimeRestrictionExtractor.BAD_PERCENT_VALUE(Percent(-1))
    verifyBadValue(bin, msg)
  }

  @Test def testNullPercentage() {
    val bin = new PsWaterVaporRestrictedBin {
      override def getValue: Integer = null
    }
    val msg = TimeRestrictionExtractor.MISSING_PERCENT_VALUE
    verifyBadValue(bin, msg)
  }

  @Test def testAbsolute() {
    val bin = mkLgsBin(42)
    val e = TimeRestrictionExtractor.extract(mkQueue(bin))
    e match {
      case \/-((Nil, List(lgs))) => {
        assertEquals(Time.hours(42), lgs.value)
      }
      case _ => fail()
    }
  }

  @Test def testNegativeTime() {
    val bin = mkLgsBin(-1)
    val msg = TimeRestrictionExtractor.BAD_TIME_VALUE(Time.hours(-1))
    verifyBadValue(bin, msg)
  }

  @Test def testNullTime() {
    val bin = new PsLgsObservationRestrictedBin {
      override def getValue: Integer = null
    }
    val msg = TimeRestrictionExtractor.MISSING_TIME_VALUE
    verifyBadValue(bin, msg)
  }

  @Test def testBoth() {
    val relbin = mkWvBin(66)
    val absbin = mkLgsBin(77)
    val e = TimeRestrictionExtractor.extract(mkQueue(absbin, relbin))
    e match {
      case \/-((List(wv), List(lgs))) => {
        assertEquals(Percent(66),    wv.value)
        assertEquals(Time.hours(77), lgs.value)
      }
      case _ => fail()
    }
  }

  @Test def testInvalidNoName() {
    val unsupported = new PsRestrictedBin() {
      override def getValue = new java.lang.Integer(88)

      def copy(value: Integer): PsRestrictedBin = this
    }

    val msg = TimeRestrictionExtractor.UNRECOGNIZED_TIME_RESTRICTION(unsupported)
    val q = mkQueue(unsupported)
    val e = TimeRestrictionExtractor.extract(q)
    e match {
      case -\/(BadData(error)) => assertEquals(msg, error)
      case _ => fail()
    }
  }

  @Test def testInvalidWithName() {
    val unsupported = new PsRestrictedBin {
      override def getDescription: String = "UNSUPPORTED"
      override def getValue = new java.lang.Integer(88)
      def copy(value: Integer): PsRestrictedBin = this
    }

    val msg = TimeRestrictionExtractor.UNRECOGNIZED_TIME_RESTRICTION(unsupported)
    val q = mkQueue(unsupported)
    val e = TimeRestrictionExtractor.extract(q)
    e match {
      case -\/(BadData(error)) => assertEquals(msg, error)
      case _ => fail()
    }
  }
}
