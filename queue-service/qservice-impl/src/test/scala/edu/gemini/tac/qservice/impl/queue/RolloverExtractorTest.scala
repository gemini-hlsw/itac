package edu.gemini.tac.qservice.impl.queue

import org.junit._
import Assert._

import edu.gemini.tac.persistence.{Committee => PsCommittee, Partner => PsPartner, Semester => PsSemester, Site => PsSite}
import edu.gemini.tac.persistence.phase1.{Condition, HmsDmsCoordinates => PsHmsDegSystem, SiderealTarget => PsTarget, TimeAmount => PsTime, TimeUnit => PsTimeUnit}
import edu.gemini.tac.persistence.phase1.sitequality.{WaterVapor, SkyBackground, CloudCover, ImageQuality}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.rollover.{AbstractRolloverObservation => PsAbstractRolloverObservation, RolloverObservation => PsRolloverObservation, RolloverSet => PsRolloverSet}
import edu.gemini.tac.psconversion._

import edu.gemini.tac.qengine.ctx.{TestPartners, Site, Semester}
import edu.gemini.tac.qengine.p1.Mode
import edu.gemini.tac.qengine.p2.{ProgramId, ObservationId}
import edu.gemini.tac.qengine.p2.rollover.RolloverReport
import edu.gemini.tac.qservice.impl.queue.{RolloverExtractor => Roll}

import edu.gemini.tac.psconversion.BadData

import scala.collection.JavaConverters._
import scalaz._
import edu.gemini.tac.qservice.impl.p1.PsFixture
import edu.gemini.tac.persistence.phase1.queues.PartnerPercentage


class RolloverExtractorTest {
  import TestPartners._

  @Test def testNoRolloverOkay() {
    val q = mkEmptyGnQueue
    Roll.extract(q) match {
      case \/-(RolloverReport(Nil)) => //ok
      case -\/(BadData(msg)) => fail(msg)
      case _ => fail()
    }
  }

  @Test def testUnspecifiedSite() {
    val q = mkEmptyGnQueue
    q.setRolloverset(new PsRolloverSet)
    Roll.extract(q) match {
      case -\/(BadData(msg)) => assertEquals(Roll.UNSPECIFIED_SITE, msg)
      case _ => fail()
    }
  }

  private def mkEmptyGnQueue: PsQueue = mkEmptyQueue(PsSite.NORTH.getDisplayName)

  private def mkEmptyQueue(site: String): PsQueue = {
    val psSite = new PsSite(site)
    val psSemester = new PsSemester() {
      override def getDisplayName = "2011A"
    }
    val c = new PsCommittee() {
      override def getSemester = psSemester
    }
    val q = new PsQueue("Test Queue Name", c) {
      override def getSite = psSite
    }

    val pps = PsFixture.psPartners.map { ps =>
      new PartnerPercentage(q, ps, ps.getPercentageShare)
    }
    q.setPartnerPercentages(pps.toSet.asJava)

    q
  }

  private def mkQueue(site: String, ro: Option[List[PsAbstractRolloverObservation]]): PsQueue = {

    val q = mkEmptyQueue(site)

    val rs = new PsRolloverSet
    rs.setSite(q.getSite)
    rs.setName("Unused")
    ro.foreach { obsList => rs.setObservations(obsList.toSet.asJava) }
    q.setRolloverset(rs)


    q
  }

  private def mkQueue(ro: PsAbstractRolloverObservation*): PsQueue =
    mkQueue(PsSite.NORTH.getDisplayName, Some(ro.toList))


  def verifyFail(q: PsQueue, expectedError: String) {
    Roll.extract(q) match {
      case -\/(BadData(msg)) => assertEquals(expectedError, msg)
      case _ => fail()
    }
  }

  @Test def testBadSite() {
    val q = mkQueue("XX", Some(Nil))
    verifyFail(q, SiteConverter.UNRECOGNIZED_SITE("XX"))
  }

  @Test def testNullObs() {
    val q = mkQueue(PsSite.NORTH.getDisplayName, None)
    verifyFail(q, Roll.UNSPECIFIED_OBSERVATIONS)
  }

  @Test def testEmptyObsOkay() {
    val q = mkQueue(PsSite.NORTH.getDisplayName, Some(Nil))

    Roll.extract(q) match {
      case \/-(RolloverReport(Nil)) => //ok
      case _ => fail()
    }
  }

  def verifyDatabaseException(ro: PsAbstractRolloverObservation): Unit = {
    val q = mkQueue(ro)
    Roll.extract(q) match {
      case -\/(DatabaseError(ex)) => // ok
      case _ => fail()
    }
  }

  def verifyFail(ro: PsAbstractRolloverObservation, expectedError: String) {
    val q = mkQueue(ro)
    verifyFail(q, expectedError)
  }

  @Test def testNullPartner() {
    val ro = new PsRolloverObservation
    verifyFail(ro, Roll.UNSPECIFIED_PARTNER)
  }

  @Test def testPartnerMissingKey() {
    val partner = new PsPartner() {
      override def getAbbreviation = "XX"
    }
    val ro = new PsRolloverObservation
    ro.setPartner(partner)
    verifyFail(ro, PartnerConverter.MISSING_PARTNER_COUNTRY_KEY)
  }

  @Test def testBadPartner() {
    val partner = new PsPartner() {
      override def getAbbreviation = "XX"
      override def getPartnerCountryKey = "XX"
    }
    val ro = new PsRolloverObservation
    ro.setPartner(partner)
    verifyFail(ro, PartnerConverter.UNRECOGNIZED_PARTNER("XX", All))
  }

  val partner = new PsPartner() {
    override def getAbbreviation = "CA"
    override def getPartnerCountryKey = "CA"
  }

  @Test def testNullObsId() {
    val ro = new PsRolloverObservation
    ro.setPartner(partner)
    verifyFail(ro, Roll.UNSPECIFIED_OBS_ID)
  }

  @Test def testBadObsId() {
    val ro = new PsRolloverObservation
    ro.setPartner(partner)
    ro.setObservationId("XX")
    verifyFail(ro, Roll.BAD_OBS_ID("XX"))
  }

  @Test def testNullTarget() {
    val ro = new PsRolloverObservation
    ro.setPartner(partner)
    ro.setObservationId("GN-2011A-Q-1-2")
    verifyFail(ro, Roll.UNSPECIFIED_TARGET)
  }

  @Test def testBadTarget() {
    val sys = new PsHmsDegSystem
    val t = new PsTarget
    t.setCoordinates(sys)

    val ro = new PsRolloverObservation
    ro.setPartner(partner)
    ro.setObservationId("GN-2011A-Q-1-2")
    ro.setTarget(t)

    // This blows up in the persistence model because it tries to parse a
    // null string for a dec :-/
    verifyDatabaseException(ro)
  }

  val sys = new PsHmsDegSystem
  sys.setRa("1:00:00")
  sys.setDec("2:00:00")

  val target = new PsTarget
  target.setCoordinates(sys)


  @Test def testNullConditions() {
    val ro = new PsRolloverObservation
    ro.setPartner(partner)
    ro.setObservationId("GN-2011A-Q-1-2")
    ro.setTarget(target)

    verifyFail(ro, Roll.UNSPECIFIED_CONDITIONS)
  }

  @Test def testMissingConditions() {
    val sq = new Condition
    sq.setWaterVapor(null)

    val ro = new PsRolloverObservation
    ro.setPartner(partner)
    ro.setObservationId("GN-2011A-Q-1-2")
    ro.setTarget(target)
    ro.setSiteQuality(sq)

    verifyDatabaseException(ro)
  }

  val siteQuality = new Condition
  siteQuality.setImageQuality(ImageQuality.IQ_85)
  siteQuality.setCloudCover(CloudCover.CC_70)
  siteQuality.setSkyBackground(SkyBackground.SB_80)
  siteQuality.setWaterVapor(WaterVapor.WV_80)

  @Test def testNullTime() {
    val ro = new PsRolloverObservation
    ro.setPartner(partner)
    ro.setObservationId("GN-2011A-Q-1-2")
    ro.setTarget(target)
    ro.setSiteQuality(siteQuality)

    verifyFail(ro, Roll.UNSPECIFIED_TIME)
  }

  val time = new PsTime(20.0, PsTimeUnit.MIN)

  @Test def testSingleObsSuccess() {
    val ro = new PsRolloverObservation
    ro.setPartner(partner)
    ro.setObservationId("GN-2011A-Q-1-2")
    ro.setTarget(target)
    ro.setSiteQuality(siteQuality)
    ro.setObservationTime(time)

    Roll.extract(mkQueue(ro)) match {
      case \/-(RolloverReport(List(obs))) =>
        assertEquals(CA, obs.partner)
        assertEquals(ObservationId(ProgramId(Site.north, new Semester(2011, Semester.Half.A), Mode.Queue, 1), 2), obs.obsId)
        assertEquals(15.0, obs.target.ra.toDeg.mag, 0.000001)
        assertEquals(2.0, obs.target.dec.toDeg.mag, 0.000001)
        assertEquals(edu.gemini.tac.qengine.p1.ImageQuality.IQ85, obs.conditions.iq)
        assertEquals(20.0, obs.time.toMinutes.value, 0.000001)
      case _ => fail()
    }
  }

  @Test def testOneBadObsRuinsAll() {
    val ro1 = new PsRolloverObservation
    ro1.setPartner(partner)
    ro1.setObservationId("GN-2011A-Q-1-2")
    ro1.setTarget(target)
    ro1.setSiteQuality(siteQuality)
    ro1.setObservationTime(time)

    val ro2 = new PsRolloverObservation

    Roll.extract(mkQueue(ro1, ro2)) match {
      case -\/(BadData(msg)) => assertEquals(Roll.UNSPECIFIED_PARTNER, msg)
      case _ => fail()
    }
  }
}