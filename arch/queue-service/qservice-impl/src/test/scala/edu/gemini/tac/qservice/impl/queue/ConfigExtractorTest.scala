package edu.gemini.tac.qservice.impl.queue

import edu.gemini.tac.persistence.{Committee => PsCommittee, Semester => PsSemester, Site => PsSite}
import edu.gemini.tac.persistence.bin.{BinConfiguration => PsBinConfiguration}
import edu.gemini.tac.persistence.phase1.queues.PartnerPercentage
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.psconversion.{PartnerSequenceConverter, BadData}

import edu.gemini.tac.qengine.api.config.ProportionalPartnerSequence
import edu.gemini.tac.qengine.ctx.Site
import edu.gemini.tac.qservice.impl.p1.PsFixture

import org.junit._
import org.junit.Assert._

import scala.collection.JavaConverters._

class ConfigExtractorTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._

  private def mkQueueWithInitialPartner(abbr: String): PsQueue = {
    val bc = new PsBinConfiguration

    val c = new PsCommittee {
      override def getSemester = new PsSemester {
        override def getDisplayName = "2011A"
      }
    }
    val q = new PsQueue("Test Queue Name", c)
    q.setSite(new PsSite {
        override def getDisplayName = PsSite.NORTH.getDisplayName
      })

    val pps = PsFixture.psPartners.map { ps =>
      new PartnerPercentage(q, ps, ps.getPercentageShare)
    }
    q.setPartnerPercentages(pps.toSet.asJava)

    q.setSubaruScheduledByQueueEngine(false)
    q.setPartnerWithInitialPick(PsFixture.psPartners.find(_.getPartnerCountryKey == abbr).get)
    q.setBinConfiguration(bc)

    q
  }

  @Test def testSuccessfulSequence() {
    val a = new ConfigExtractor(mkQueueWithInitialPartner("BR"))
    val sequence = a.sequence
    assertEquals(BR, sequence.toOption.get.sequence.head)
  }

  @Test def testSuccessfulDefault() {
    val q = mkQueueWithInitialPartner("CL")
    q.setPartnerWithInitialPick(null)

    val a = new ConfigExtractor(q)
    val p0 = new ProportionalPartnerSequence(All, Site.north).sequence.head
    val p1 = a.sequence.toOption.get.sequence.head
    println(p0)
    println(p1)
    assertEquals(new ProportionalPartnerSequence(All, Site.north).sequence.head, a.sequence.toOption.get.sequence.head)
  }

  @Test(expected=classOf[NoSuchElementException]) def testUnrecognizedInitialPartner() {
    new ConfigExtractor(mkQueueWithInitialPartner("XX"))
  }

  @Test def testInappropriateInitialPartnerForSite() {
    val a = new ConfigExtractor(mkQueueWithInitialPartner("CL"))
    assertEquals(BadData(PartnerSequenceConverter.BAD_INITIAL_PICK(CL, Site.north)), a.sequence.swap.toOption.get)
  }

  @Test(expected=classOf[NoSuchElementException]) def testNullPartnerName() {
    new ConfigExtractor(mkQueueWithInitialPartner(null))
  }
}
