package edu.gemini.tac.qservice.impl.queue

import org.junit._
import Assert._
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.{Committee => PsCommittee}
import edu.gemini.tac.persistence.bandrestriction.{BandRestrictionRule => PsBandRestriction}
import edu.gemini.tac.persistence.bandrestriction.{Iq20RestrictionNotInBand3 => PsIQ20BandRestriction}
import edu.gemini.tac.persistence.bandrestriction.{LgsRestrictionInBandsOneAndTwo => PsLgsBandRestriction}
import edu.gemini.tac.persistence.bandrestriction.{RapidTooRestrictionInBandOne => PsRapidTooBandRestriction}
import edu.gemini.tac.persistence.{Band, Proposal}
import edu.gemini.tac.psconversion.BadData

import edu.gemini.tac.qengine.api.config.BandRestriction
import edu.gemini.tac.qengine.api.config.Default._

import scala.collection.JavaConverters._

import scalaz._

class BandRestrictionExtractorTest {
  private def mkQueue(r: PsBandRestriction*): PsQueue = {
    val s: Set[PsBandRestriction] = Set(r: _*)
    val c = new PsCommittee
    val q = new PsQueue("Test Queue Name", c)
    q.setBandRestrictionRules(s.asJava)
    q
  }

  @Test def testNone() {
    val e = BandRestrictionExtractor.extract(mkQueue())
    e match {
      case \/-(List(`NotBand3Restriction`)) => // ok
      case -\/(BadData(msg)) => fail(msg)
    }
  }

  @Test def testOne() {
    val e = BandRestrictionExtractor.extract(mkQueue(new PsIQ20BandRestriction))
    e match {
      case \/-(List(`NotBand3Restriction`, `Iq20BandRestriction`)) => // ok
      case _ => fail()
    }
  }

  @Test def testAll() {
    val q  = mkQueue(new PsIQ20BandRestriction, new PsLgsBandRestriction, new PsRapidTooBandRestriction)
    val e  = BandRestrictionExtractor.extract(q)
    e match {
      case \/-(List(nb3, iq, lgs, too)) => {
        assertEquals(BandRestriction.NotBand3Name, nb3.name)
        assertEquals(BandRestriction.Iq20Name,     iq.name)
        assertEquals(BandRestriction.LgsName,      lgs.name)
        assertEquals(BandRestriction.RapidTooName, too.name)
      }
      case _ => fail()
    }
  }

  @Test def testInvalidNoName() {
    val unsupported = new PsBandRestriction {
    }

    val msg = BandRestrictionExtractor.UNRECOGNIZED_BAND_RESTRICTION(unsupported)
    val q = mkQueue(unsupported)
    val e = BandRestrictionExtractor.extract(q)
    e match {
      case -\/(BadData(error)) => assertEquals(msg, error)
      case _ => fail()
    }
  }

  @Test def testInvalidWithName() {
    val unsupported = new PsBandRestriction {
      override def getName: String = "UNSUPPORTED"
    }

    val msg = BandRestrictionExtractor.UNRECOGNIZED_BAND_RESTRICTION(unsupported)
    val q = mkQueue(unsupported)
    val e = BandRestrictionExtractor.extract(q)
    e match {
      case -\/(BadData(error)) => {
        assertEquals(msg, error)
        assertTrue(error.contains("UNSUPPORTED"))
      }
      case _ => fail()
    }
  }
}
