package edu.gemini.tac.qservice.impl.queue

import org.junit._
import Assert._

import edu.gemini.tac.persistence.bin.{RABinSize => PsRaBinSize}

import RaBinSizeExtractor._
import scalaz._
import edu.gemini.tac.psconversion.BadData

class RaBinSizeExtractorTest {
  @Test def testNoHours() {
    val psSize = new PsRaBinSize

    extract(psSize) match {
      case -\/(BadData(msg)) => assertEquals(UNSPECIFIED_SIZE_HOURS, msg)
      case _ => fail()
    }
  }

  @Test def testNoMinutes() {
    val psSize = new PsRaBinSize
    psSize.setHours(1)
    extract(psSize) match {
      case -\/(BadData(msg)) => assertEquals(UNSPECIFIED_SIZE_MINUTES, msg)
      case _ => fail()
    }
  }

  @Test def testNegativeHours() {
    val psSize = new PsRaBinSize
    psSize.setHours(-1)
    psSize.setMinutes(30)
    extract(psSize) match {
      case -\/(BadData(msg)) => assertEquals(NEGATIVE_SIZE(-1), msg)
      case _ => fail()
    }
  }

  @Test def testNegativeMinutes() {
    val psSize = new PsRaBinSize
    psSize.setHours(1)
    psSize.setMinutes(-30)
    extract(psSize) match {
      case -\/(BadData(msg)) => assertEquals(NEGATIVE_SIZE(-30), msg)
      case _ => fail()
    }
  }

  @Test def testZero() {
    val psSize = new PsRaBinSize
    psSize.setHours(0)
    psSize.setMinutes(0)
    extract(psSize) match {
      case -\/(BadData(msg)) => assertEquals(ILLEGAL_SIZE(0), msg)
      case _ => fail()
    }
  }

  @Test def testIllegalSize() {
    val psSize = new PsRaBinSize
    psSize.setHours(1)
    psSize.setMinutes(17)
    extract(psSize) match {
      case -\/(BadData(msg)) => assertEquals(ILLEGAL_SIZE(77), msg)
      case _ => fail()
    }
  }

  @Test def testGoodSize() {
    val psSize = new PsRaBinSize
    psSize.setHours(1)
    psSize.setMinutes(0)
    extract(psSize) match {
      case \/-(sz) => assertEquals(60, sz.getSize)
      case _ => fail()
    }
  }
}