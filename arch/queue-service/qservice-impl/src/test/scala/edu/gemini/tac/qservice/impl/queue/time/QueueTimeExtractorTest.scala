package edu.gemini.tac.qservice.impl.queue.time

import org.junit._
import Assert._
import scala.collection.JavaConverters._
import edu.gemini.tac.persistence.{Partner => PsPartner, Site => PsSite, Semester => PsSemester}
import edu.gemini.tac.persistence.{Committee => PsCommittee}
import edu.gemini.tac.persistence.bin.{BinConfiguration => PsBinConfiguration}
import edu.gemini.tac.persistence.phase1.{TimeAmount => PsTime, TimeUnit => PsTimeUnit}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}

import edu.gemini.tac.qengine.api.config.QueueBandPercentages
import edu.gemini.tac.psconversion.test.PartnerFactory._
import edu.gemini.tac.psconversion.{Partners, PartnerConverter, BadData}
import edu.gemini.tac.persistence.queues.partnerCharges.{ExchangePartnerCharge => PsExchangePartnerCharge}
import edu.gemini.tac.qengine.p2.rollover.{RolloverObservation, RolloverReport}
import edu.gemini.tac.qengine.p2.ObservationId
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.util.{Percent, Time}
import edu.gemini.tac.qengine.ctx.{TestPartners, Partner, Site}
import edu.gemini.tac.persistence.phase1.queues.PartnerPercentage

import scalaz._
import edu.gemini.tac.qservice.impl.p1.PsFixture


class QueueTimeExtractorTest{
  val partners = PsFixture.partners

  import TestPartners._

  @Test def specifyBandPercentages(): Unit = {
    val a = new QueueTimeExtractor(mkQueueWithBandPercentages(20, 70, 90), partners)
    assertEquals(QueueBandPercentages(20, 50, 20), a.bandPercentages.toOption.get)
  }

  @Test def overrideDefaultOverfill(): Unit = {
    // something other than the default
    val a1 = new QueueTimeExtractor(mkQueueWithBandPercentages(30, 60, 80, 6), partners)
    assertEquals(Percent(6), a1.overfillFactor.toOption.get)

    // 0 okay
    val a2 = new QueueTimeExtractor(mkQueueWithBandPercentages(30, 60, 80, 0), partners)
    assertEquals(Percent(0), a2.overfillFactor.toOption.get)
  }

  @Test def cannotSetFaultyBandPercentages() {
    // Negative Band 1 cutoff
    val a1 = new QueueTimeExtractor(mkQueueWithBandPercentages(-1, 70, 90), partners)
    failBandPercentages(a1, QueueTimeExtractor.BAD_BAND_PERCENTAGES(-1, 70, 90))

    // Band 2 cutoff before band 1 cutoff
    val a2 = new QueueTimeExtractor(mkQueueWithBandPercentages(20, 10, 90), partners)
    failBandPercentages(a2, QueueTimeExtractor.BAD_BAND_PERCENTAGES(20, 10, 90))

    // Band 3 cutoff before band 2 cutoff
    val a3 = new QueueTimeExtractor(mkQueueWithBandPercentages(10, 20, 15), partners)
    failBandPercentages(a3, QueueTimeExtractor.BAD_BAND_PERCENTAGES(10, 20, 15))

    // Band 3 cutoff over 100
    val a4 = new QueueTimeExtractor(mkQueueWithBandPercentages(101, 102, 103), partners)
    failBandPercentages(a4, QueueTimeExtractor.BAD_BAND_PERCENTAGES(101, 102, 103))
  }

  @Test def cannotSetOverfillToNegativeValue() {
    // Negative Band 1 cutoff
    val a1 = new QueueTimeExtractor(mkQueueWithBandPercentages(30, 60, 80, -1), partners)
    a1.overfillFactor match {
      case -\/(BadData(msg)) => assertEquals(msg, QueueTimeExtractor.NEGATIVE_OVERFILL_FACTOR(-1))
      case _ => fail()
    }
  }

  @Test def cannotHaveNullBandCutoffs() {
    // Make a queue with null cutoffs
    val q = mkQueue
    q.setBand1Cutoff(null)
    q.setBand2Cutoff(null)
    q.setBand3Cutoff(null)
    q.setBinConfiguration(binConf)

    assertNull(q.getBand1Cutoff)
    assertNull(q.getBand2Cutoff)
    assertNull(q.getBand3Cutoff)

    q.setTotalTimeAvailable(100)

    val a1 = new QueueTimeExtractor(q, partners)
    failBandPercentages(a1, QueueTimeExtractor.BAD_BAND1_CUTOFF)
    assertTrue(a1.extract.isLeft)

    q.setBand1Cutoff(new java.lang.Integer(30))
    val a2 = new QueueTimeExtractor(q, partners)
    failBandPercentages(a2, QueueTimeExtractor.BAD_BAND2_CUTOFF)
    assertTrue(a2.extract.isLeft)

    q.setBand2Cutoff(new java.lang.Integer(60))
    val a3 = new QueueTimeExtractor(q, partners)
    failBandPercentages(a3, QueueTimeExtractor.BAD_BAND3_CUTOFF)
    assertTrue(a3.extract.isLeft)
  }


  @Test def cannotHaveNullTotalTime() {
    val q = mkQueue
    // No rollover time
    //    q.setRolloverAllocation(new java.util.HashMap[PsPartner, java.lang.Integer])
    q.setTotalTimeAvailable(null)
    q.setBinConfiguration(binConf)

    val a = new QueueTimeExtractor(q, partners)
    a.extract match {
      case -\/(BadData(msg)) => assertEquals(QueueTimeExtractor.BAD_PARTNER_TIME, msg)
      case _ => fail()
    }
  }

  @Test def calculatesDefaultBasePartnerTimeCorrectly() {
    val q = mkQueue
    q.setTotalTimeAvailable(100)
    // No rollover time
    //    q.setRolloverAllocation(new java.util.HashMap[PsPartner, java.lang.Integer])
    q.setBinConfiguration(binConf)

    val a = new QueueTimeExtractor(q, partners)
    val m = a.extract.toOption.get.fullPartnerTime
    assertEquals(US.percentDoubleAt(Site.north), m(US).toHours.value, 0.000001)
    assertEquals(BR.percentDoubleAt(Site.north), m(BR).toHours.value, 0.000001)
    partners.foreach {
      p => assertEquals("Wrong percentage for " + p.id , p.percentDoubleAt(Site.north), m(p).toHours.value, Double.MinValue)
    }

    assertEquals(q.getBand1Cutoff.intValue, a.bandPercentages.toOption.get.band1.doubleValue, Double.MinPositiveValue)
    assertEquals(q.getBand2Cutoff.intValue, a.bandPercentages.map {
      bp => bp(QueueBand.Category.B1_2)
    }.toOption.get.doubleValue, Double.MinPositiveValue)
    assertEquals(q.getBand3Cutoff.intValue, a.bandPercentages.map {
      bp => bp(QueueBand.Category.Guaranteed)
    }.toOption.get.doubleValue, Double.MinPositiveValue)
  }

  @Test def calculatesOverriddenBasePartnerTimeCorrectly(){
    val q = mkQueue
    q.setTotalTimeAvailable(100)
    q.setBinConfiguration(binConf)

    //Override default
    val partnerPercentages = q.getPartnerPercentages
    val trimmed = partnerPercentages.asScala.filter(_.partner.getPartnerCountryKey != "BR").filter(_.partner.getPartnerCountryKey != "US")
    val modded = trimmed ++ Set(mkPartnerPercentage("BR", .9), mkPartnerPercentage("US", .1))
    q.setPartnerPercentages(modded.asJava)

    val p = Partners.fromQueue(q).toOption.get
    val us = p.values.find(_.id == "US").get
    val br = p.values.find(_.id == "BR").get

    val a = new QueueTimeExtractor(q, p.values)
    val m = a.extract.toOption.get.fullPartnerTime
    assertEquals(10, m(us).toHours.value, Double.MinValue)
    assertEquals(90, m(br).toHours.value, Double.MinValue)
    q.getPartnerPercentages.asScala.foreach { pp : PartnerPercentage =>
      assertEquals("Wrong percentage for " + pp.getPartner.getPartnerCountryKey, pp.getPercentage * 100.0, m(PartnerConverter.find(pp.getPartner, p.values).toOption.get).toHours.value, 0.0001)
    }

    assertEquals(q.getBand1Cutoff.intValue, a.bandPercentages.toOption.get.band1.doubleValue, Double.MinPositiveValue)
    assertEquals(q.getBand2Cutoff.intValue, a.bandPercentages.map {
      bp => bp(QueueBand.Category.B1_2)
    }.toOption.get.doubleValue, Double.MinPositiveValue)
    assertEquals(q.getBand3Cutoff.intValue, a.bandPercentages.map {
      bp => bp(QueueBand.Category.Guaranteed)
    }.toOption.get.doubleValue, Double.MinPositiveValue)
  }



  @Test def calculatesQueueTimeCorrectly() {
    val q = mkQueue
    q.setTotalTimeAvailable(100)
    q.setBinConfiguration(binConf)

    // A US classical proposal, which will reduce the queue time for the US.
    val us = mkProp(US, "us-1", Time.hours(5), Site.north, Mode.Classical)

    // 1 hour of exchange time for Brazil
    val exchangeCharges = new java.util.HashMap[PsPartner, PsExchangePartnerCharge]
    val psBR = mkPsPartner("BR")
    exchangeCharges.put(psBR, new PsExchangePartnerCharge(q, psBR, new PsTime(1.0, PsTimeUnit.HR)))
    q.setExchangePartnerCharges(exchangeCharges)

    val ro = RolloverObservation(
      CA,
      ObservationId.parse("GN-2011A-Q-1-2").get,
      Target(15.0, 2.0),
      ObsConditions.AnyConditions,
      Time.hours(10)
    )
    val rop = RolloverReport(List(ro))

    def roTime(p: Partner): Double = p.percentDoubleAt(Site.north)/10.0

    val a = new QueueTimeExtractor(q, partners, rop, List(us))
    val m = a.extract.toOption.get.fullPartnerTime
    assertSameTime(US.percentDoubleAt(Site.north) - 5.0 - roTime(US), m(US))
    assertSameTime(AR.percentDoubleAt(Site.north) - roTime(AR), m(AR))
    assertSameTime(BR.percentDoubleAt(Site.north) - 1.0 - roTime(BR), m(BR))
    assertSameTime(UH.percentDoubleAt(Site.north) - roTime(UH), m(UH))

    val ptc = a.partnerTimeCalc.toOption.get
    assertSameTime(US.percentDoubleAt(Site.north), ptc.base(US))
    assertSameTime(AR.percentDoubleAt(Site.north), ptc.base(AR))
    assertSameTime( 5.0, ptc.classical(US))
    assertSameTime( 0.0, ptc.classical(UH))
    assertSameTime( 1.0, ptc.exchange(BR))
    assertSameTime( 0.0, ptc.exchange(US))
    assertSameTime( 0.0, ptc.exchange(UH))
    assertSameTime(roTime(CA), ptc.rollover(CA))
    assertSameTime(roTime(US), ptc.rollover(US))
    assertSameTime(roTime(UH), ptc.rollover(UH))
  }

  private def failBandPercentages(e: QueueTimeExtractor, expected: String) {
    e.bandPercentages match {
      case -\/(BadData(msg)) => assertEquals(expected, msg)
      case _ => fail()
    }
  }


  private def binConf: PsBinConfiguration =
    new PsBinConfiguration


  private def mkProp(p: Partner, id: String, t: Time, s: Site, m: Mode): Proposal = {
    val ntac = Ntac(p, id, 1.0, t)
    CoreProposal(ntac, s, m)
  }

  private def assertSameTime(hrs: Double, t: Time) {
    assertEquals(hrs, t.toHours.value, 0.000001)
  }

  private def mkPartnerPercentage(countryKey : String,  proportion : Double) : PartnerPercentage = {
    val partner = mkPsPartner(countryKey)
    new PartnerPercentage(null, partner, proportion)
  }

  private def mkQueue(overfill: Int): PsQueue = {
    val site = new PsSite {
      override def getDisplayName: String = PsSite.NORTH.getDisplayName
    }
    val semester = new PsSemester {
      override def getDisplayName: String = "2011A"
    }

    val c = new PsCommittee() {
      override def getSemester = semester
    }

    val q = new PsQueue("Test Queue Name", c)

    q.setSite(site)
    q.setOverfillLimit(overfill)
    q.setPartnerPercentages(java.util.Collections.emptySet[PartnerPercentage]())
    q.getPartnerPercentages.asScala.map(_.setQueue(q))
    q.setSubaruScheduledByQueueEngine(false)
    q
  }

  private def mkQueue: PsQueue = mkQueue(5)

  private def mkQueueWithBandPercentages(b1Cut: Int, b2Cut: Int, b3Cut: Int, overfill: Int = 5): PsQueue = {
    val q = mkQueue(overfill)
    q.setBand1Cutoff(b1Cut)
    q.setBand2Cutoff(b2Cut)
    q.setBand3Cutoff(b3Cut)
    q
  }



}