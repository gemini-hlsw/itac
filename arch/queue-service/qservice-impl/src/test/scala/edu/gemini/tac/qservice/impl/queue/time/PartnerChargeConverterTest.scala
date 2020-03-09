package edu.gemini.tac.qservice.impl.queue.time

import org.junit._
import Assert._
import edu.gemini.tac.persistence.{Partner => PsPartner}
import edu.gemini.tac.persistence.queues.partnerCharges.{ExchangePartnerCharge => PsExchangePartnerCharge}
import edu.gemini.tac.persistence.phase1.{TimeAmount => PsTime, TimeUnit => PsTimeUnit}
import edu.gemini.tac.qengine.util.Time

import collection.JavaConverters._
import edu.gemini.tac.psconversion.test.PartnerFactory
import edu.gemini.tac.psconversion.{BadData, TimeConverter}

import scalaz._

class PartnerChargeConverterTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._

  @Test def testNullPartnerCharge() {
    val pcc = new PartnerChargeConverter("Exchange", null, All)
    pcc.partnerTime match {
      case -\/(BadData(msg)) => assertEquals(pcc.MISSING_CONFIG, msg)
      case _ => fail()
    }
  }

  private def mkMap(tups: Traversable[(PsPartner, PsTime)]): java.util.Map[PsPartner, PsExchangePartnerCharge] =
    tups.map { case (psPartner, psTime) =>
        psPartner -> new PsExchangePartnerCharge(null, psPartner, psTime)
    }.toMap.asJava

  private def mkPcc(tups: (PsPartner, PsTime)*) =
    new PartnerChargeConverter("Exchange", mkMap(tups), All)

  @Test def testEmptyMap() {
    mkPcc().partnerTime match {
      case \/-(pt) => assertEquals(Time.Zero, pt.total)
      case _ => fail()
    }
  }

  @Test def testMissingPartner() {
    val us = PartnerFactory.mkPsPartner("US")
    val pc = new PsExchangePartnerCharge(null, null, new PsTime(1.0, PsTimeUnit.HR))
    val m  = new java.util.HashMap[PsPartner, PsExchangePartnerCharge]
    m.put(us, pc)
    val pcc = new PartnerChargeConverter("Exchange", m, All)

    pcc.partnerTime match {
      case -\/(BadData(msg)) => assertEquals(pcc.MISSING_PARTNER, msg)
      case _ => fail()
    }
  }

  @Test def testUnrecognizedPartner() {
    val xx   = PartnerFactory.mkPsPartner("XX")
    val time = new PsTime(1.0, PsTimeUnit.HR)
    val pcc  = mkPcc(xx -> time)
    pcc.partnerTime match {
      case -\/(BadData(msg)) => assertEquals(pcc.UNRECOGNIZED_PARTNER(xx), msg)
      case _ => fail()
    }
  }

  @Test def testMissingTime() {
    val us = PartnerFactory.mkPsPartner("US")
    val pcc = mkPcc((us, null))
    pcc.partnerTime match {
      case -\/(BadData(msg)) => assertEquals(pcc.MISSING_TIME, msg)
      case _ => fail()
    }
  }

  @Test def testMissingUnits() {
    val us = PartnerFactory.mkPsPartner("US")
    val pcc = mkPcc(us -> new PsTime(1.0, null))
    pcc.partnerTime match {
      case -\/(BadData(msg)) => assertEquals(TimeConverter.MISSING_TIME_UNITS, msg)
      case _ => fail()
    }
  }

  @Test def testValidPartnerCharges() {
    val ca = PartnerFactory.mkPsPartner("CA")
    val us = PartnerFactory.mkPsPartner("US")
    val pcc = mkPcc(ca -> new PsTime(1.0, PsTimeUnit.HR), us -> new PsTime(2.0, PsTimeUnit.MIN))

    pcc.partnerTime match {
      case \/-(pt) =>
        assertEquals(Time.hours(1.0), pt(CA))
        assertEquals(Time.minutes(2.0), pt(US))
        assertEquals(Time.minutes(62.0), pt.total)
      case _ => fail()
    }
  }
}