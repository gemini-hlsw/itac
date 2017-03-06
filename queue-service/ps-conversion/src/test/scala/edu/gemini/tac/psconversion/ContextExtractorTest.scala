package edu.gemini.tac.psconversion

import org.junit._
import Assert._

import edu.gemini.tac.persistence.{Semester => PsSemester}
import edu.gemini.tac.persistence.{Site => PsSite}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.{Committee => PsCommittee}
import edu.gemini.tac.qengine.ctx.{Context, Site, Semester}
import scalaz._

class ContextExtractorTest {

  private def mkSite(name: String): PsSite =
    new PsSite {
      override def getDisplayName: String = name
    }

  private def mkSemester(name: String): PsSemester =
    new PsSemester {
      override def getDisplayName: String = name
    }

  @Test def testNoSite() {
    val q = new PsQueue("XYZ", new PsCommittee())
    SiteConverter.read(q) match {
      case -\/(BadData(msg)) => assertEquals(SiteConverter.UNSPECIFIED_SITE, msg)
      case _ => fail()
    }
  }

  @Test def testNoSemester() {
    val q = new PsQueue("XYZ", new PsCommittee())
    SemesterConverter.read(q) match {
      case -\/(BadData(msg)) => assertEquals(SemesterConverter.UNSPECIFIED_SEMESTER, msg)
      case _ => fail()
    }
  }

  @Test def testNoSiteDisplayName() {
    val site = new PsSite {
      override def getDisplayName: String = null
    }

    val q = new PsQueue("XYZ", new PsCommittee()) {
      override def getSite = site
    }

    SiteConverter.read(q) match {
      case -\/(BadData(msg)) => assertEquals(SiteConverter.UNSPECIFIED_SITE, msg)
      case _ => fail()
    }
  }

  @Test def testGoodSite() {
    val q = new PsQueue("XYZ", new PsCommittee()) {
      override def getSite = mkSite("Gemini South")
    }

    SiteConverter.read(q) match {
      case \/-(Site.south) => // ok
      case _ => fail()
    }
  }

  @Test def testBadSite() {
    val q = new PsQueue("XYZ", new PsCommittee()) {
      override def getSite = mkSite("Gemini West")
    }

    SiteConverter.read(q) match {
      case -\/(BadData(msg)) => assertEquals(SiteConverter.UNRECOGNIZED_SITE("Gemini West"), msg)
      case _ => fail()
    }
  }

  @Test def testNoSemesterString() {
    val sem = new PsSemester
    val committee = new PsCommittee() {
      override def getSemester = sem
    }
    val q = new PsQueue("XYZ", committee)

    SemesterConverter.read(q) match {
      case -\/(BadData(msg)) => assertEquals(SemesterConverter.UNSPECIFIED_SEMESTER, msg)
      case _ => fail()
    }
  }

  @Test def testGoodSemester() {
    val committee = new PsCommittee() {
      override def getSemester = mkSemester("2010A")
    }
    val q = new PsQueue("XYZ", committee)

    SemesterConverter.read(q) match {
      case \/-(sem) => assertEquals(new Semester(2010, Semester.Half.A), sem)
      case _ => fail()
    }
  }

  @Test def testBadSemester() {
    val committee = new PsCommittee() {
      override def getSemester = mkSemester("2010Z")
    }
    val q = new PsQueue("XYZ", committee)

    SemesterConverter.read(q) match {
      case -\/(BadData(msg)) => assertEquals(SemesterConverter.UNRECOGNIZED_SEMESTER("2010Z"), msg)
      case _ => fail()
    }
  }
  @Test def testGoodContext() {
    val site = mkSite("Gemini South")
    val sem  = mkSemester("2010A")

    val committee = new PsCommittee() {
      override def getSemester = sem
    }
    val q = new PsQueue("XYZ", committee) {
      override def getSite = site
    }

    ContextConverter.read(q) match {
      case \/-(ctx) => assertEquals(new Context(Site.south, new Semester(2010, Semester.Half.A)), ctx)
      case _ => fail()
    }
  }
}