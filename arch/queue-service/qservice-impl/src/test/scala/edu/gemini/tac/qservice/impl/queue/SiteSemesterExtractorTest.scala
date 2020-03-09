package edu.gemini.tac.qservice.impl.queue

import org.junit._
import Assert._

import edu.gemini.tac.persistence.{Semester => PsSemester}
import edu.gemini.tac.persistence.{Site => PsSite}
import edu.gemini.tac.persistence.bin.{BinConfiguration => PsBinConfiguration}
import edu.gemini.tac.persistence.bin.{DecBinSize => PsDecBinSize}
import edu.gemini.tac.persistence.bin.{RABinSize => PsRaBinSize}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.{Committee => PsCommittee}

import SiteSemesterExtractor._
import edu.gemini.qengine.skycalc.{DecBinSize, RaBinSize}
import edu.gemini.tac.qengine.api.config.SiteSemesterConfig
import edu.gemini.tac.psconversion.{PsError, BadData}
import edu.gemini.tac.qengine.ctx.{Semester, Site, Context}
import edu.gemini.tac.persistence.daterange.{DateRangePersister, Shutdown => PsShutdown}
import edu.gemini.shared.util.DateRange
import org.joda.time.{Period, DateTime}

import scalaz._

class SiteSemesterExtractorTest {

  private def mkSite(name: String): PsSite =
    new PsSite {
      override def getDisplayName: String = name
    }

  private def mkSemester(name: String): PsSemester =
    new PsSemester {
      override def getDisplayName: String = name
    }

  @Test def testNoBinConfig() {
    val c = new PsCommittee {
      override def getSemester = mkSemester("2011A")
    }
    val queue = new PsQueue("Test Queue Name", c) {
      override def getSite = mkSite("Gemini South")
    }
    extract(queue) match {
      case -\/(BadData(msg)) => assertEquals(UNSPECIFIED_BIN_CONFIG, msg)
      case _ => fail()
    }
  }

  private def mkBinConf: PsBinConfiguration =
    new PsBinConfiguration

//  private val ctx = new Context(mkSite("Gemini South"), mkSemester("2010A"))

  @Test def testNoRaBinSize() {
    extractRaBinSize(mkBinConf) match {
      case -\/(BadData(msg)) => assertEquals(UNSPECIFIED_RA_SIZE, msg)
      case _ => fail()
    }
  }

  private def mkRaBinSize(hrs: Int, mins: Int): PsRaBinSize = {
    val sz = new PsRaBinSize
    sz.setHours(hrs)
    sz.setMinutes(mins)
    sz
  }

  @Test def testBadRaBinSize() {
    val bc = mkBinConf
    bc.setRaBinSize(mkRaBinSize(17, 13))

    extractRaBinSize(bc) match {
      case -\/(BadData(msg)) => assertEquals(RaBinSizeExtractor.ILLEGAL_SIZE(17*60+13), msg)
      case _ => fail()
    }
  }

  @Test def testGoodRaBinSize() {
    val bc = mkBinConf
    bc.setRaBinSize(mkRaBinSize(1, 30))

    extractRaBinSize(bc) match {
      case \/-(sz) => assertEquals(new RaBinSize(90), sz)
      case _ => fail()
    }
  }

  @Test def testNoDecSize() {
    val bc = mkBinConf
    extractDecBinSize(bc) match {
      case -\/(BadData(msg)) => assertEquals(UNSPECIFIED_DEC_SIZE, msg)
      case _ => ()
    }
  }

  private def mkDecBinSize(deg: Int): PsDecBinSize = {
    val sz = new PsDecBinSize
    sz.setDegrees(deg)
    sz
  }

  @Test def testBadDecBinSize() {
    val bc = mkBinConf
    bc.setDecBinSize(mkDecBinSize(17))

    extractDecBinSize(bc) match {
      case -\/(BadData(msg)) => assertEquals(DecBinSizeExtractor.ILLEGAL_SIZE(17), msg)
      case _ => fail()
    }
  }

  @Test def testGoodDecBinSize() {
    val bc = mkBinConf
    bc.setDecBinSize(mkDecBinSize(10))

    extractDecBinSize(bc) match {
      case \/-(sz) => assertEquals(new DecBinSize(10), sz)
      case _ => fail()
    }
  }

  private def mkGoodBinConfig: PsBinConfiguration = {
    val bc = mkBinConf
    bc.setRaBinSize(mkRaBinSize(2, 0))
    bc.setDecBinSize(mkDecBinSize(20))
    bc
  }

  private def validateGoodConfig(e: PsError \/ SiteSemesterConfig) {
    e match {
      case \/-(conf) => {
        // Don't know how much time to expect per bin....  Lame comparison to
        // at least see that we get the right number of bins for 2 hour bins.
        assertEquals(12, conf.raLimits.bins.size)

        // Ditto for dec percents
        assertEquals(9, conf.decLimits.bins.size)
      }
      case _ => fail()
    }
  }

  @Test def testGoodBinConfig() {
    validateGoodConfig(extract(new Context(Site.south, new Semester(2010, Semester.Half.A)), mkGoodBinConfig, List.empty))
  }

  @Test def testGoodBinQueue() {
    val c = new PsCommittee() {
      override def getSemester = mkSemester("2010A")
    }
    val queue = new PsQueue("Test Queue Name", c) {
      override def getSite = mkSite("Gemini South")
    }
    queue.setBinConfiguration(mkGoodBinConfig)
    validateGoodConfig(extract(queue))
  }

  @Test
  def shutdownsAreSubtracted(){
    val c = new PsCommittee() {
      override def getSemester = mkSemester("2012B")
    }
    val start = new DateTime(2012, 3, 26, 12, 0, 0, 0)
    val stop = start.plus(Period.days(1))
    val dr = new DateRange(start.toDate, stop.toDate)
    val drp = new DateRangePersister(dr)
    val s = new PsShutdown(drp, mkSite("Gemini South"), c)
    val ss = List(s)
    validateGoodConfig(extract(new Context(Site.south, new Semester(2010, Semester.Half.A)), mkGoodBinConfig, ss))
  }
}
