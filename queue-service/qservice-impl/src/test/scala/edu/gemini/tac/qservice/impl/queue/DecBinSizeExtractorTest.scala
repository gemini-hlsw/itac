package edu.gemini.tac.qservice.impl.queue

import org.junit._
import Assert._

import edu.gemini.tac.persistence.bin.{DecBinSize => PsDecBinSize}
import edu.gemini.tac.psconversion.BadData

import DecBinSizeExtractor._

import scalaz._

class DecBinSizeExtractorTest {
  @Test def testNoDegrees() {
    val psSize = new PsDecBinSize

    extract(psSize) match {
      case -\/(BadData(msg)) => assertEquals(UNSPECIFIED_SIZE, msg)
      case _ => fail()
    }
  }

  @Test def testNegativeDegrees() {
    val psSize = new PsDecBinSize
    psSize.setDegrees(-10)
    extract(psSize) match {
      case -\/(BadData(msg)) => assertEquals(NEGATIVE_SIZE(-10), msg)
      case _ => fail()
    }
  }

  @Test def testZero() {
    val psSize = new PsDecBinSize
    psSize.setDegrees(0)
    extract(psSize) match {
      case -\/(BadData(msg)) => assertEquals(ILLEGAL_SIZE(0), msg)
      case _ => fail()
    }
  }

  @Test def testIllegalSize() {
    val psSize = new PsDecBinSize
    psSize.setDegrees(17)
    extract(psSize) match {
      case -\/(BadData(msg)) => assertEquals(ILLEGAL_SIZE(17), msg)
      case _ => fail()
    }
  }

  @Test def testGoodSize() {
    val psSize = new PsDecBinSize
    psSize.setDegrees(20)
    extract(psSize) match {
      case \/-(sz) => assertEquals(20, sz.getSize)
      case _ => fail()
    }
  }
}