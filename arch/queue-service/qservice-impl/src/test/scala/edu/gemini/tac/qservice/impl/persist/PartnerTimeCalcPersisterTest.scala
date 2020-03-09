package edu.gemini.tac.qservice.impl.persist

import org.junit._
import Assert._


import edu.gemini.tac.persistence.{Partner => PsPartner, Site => PsSite}
import edu.gemini.tac.persistence.queues.partnerCharges.{PartnerCharge => PsPartnerCharge}
import edu.gemini.tac.persistence.queues.partnerCharges.{AvailablePartnerTime      => PsAvailableTime}
import edu.gemini.tac.persistence.queues.partnerCharges.{ClassicalPartnerCharge    => PsClassicalTime}
import edu.gemini.tac.persistence.queues.partnerCharges.{RolloverPartnerCharge     => PsRolloverPartnerCharge}


import scala.collection.JavaConverters._

import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.api.queue.time.{PartnerTimeCalc, PartnerTime}
import edu.gemini.tac.qservice.impl.fixture.FixedQueue
import edu.gemini.tac.qengine.ctx.Site
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.psconversion.Partners

class PartnerTimeCalcPersisterTest {

  val (queue, committee) = FixedQueue.mkQueue
  val partners = Partners.fromQueue(queue).toOption.get

  val site = Site.south

  private def verify(b: PartnerTime, c: PartnerTime, r: PartnerTime) {
    val empty = PartnerTime.empty(partners.values)
    val ptc = PartnerTimeCalc(partners.values, b, classical = c, rollover = r, exchange = empty, adjustment = empty, partnerTrade = empty)
    PartnerTimeCalcPersister.persist(ptc, partners.psPartnerFor, queue)
    verify(ptc, queue)
  }

  @Test def testTimesMapToPsValuesCorrectly() {
    val b = PartnerTime.distribute(Time.hours(1000), site, partners.values)
    val c = PartnerTime.distribute(Time.hours( 100), site, partners.values)
    val r = PartnerTime.distribute(Time.hours(   1), site, partners.values)
    verify(b,c,r)
  }

  @Test def timesMapToHours() {
    val b = PartnerTime.distribute(Time.minutes(1000), site, partners.values)
    val c = PartnerTime.distribute(Time.minutes( 100), site, partners.values)
    val r = PartnerTime.distribute(Time.minutes(   1), site, partners.values)
    verify(b,c,r)
  }

  @Test def testMissingCategoryWorks() {
    val b = PartnerTime.distribute(Time.minutes(1000), site, partners.values)
    val c = PartnerTime.distribute(Time.minutes( 100), site, partners.values)
    val r = PartnerTime.empty(partners.values)
    verify(b,c,r)
  }

  def verify(ptc: PartnerTimeCalc, q: PsQueue) {
    verify[PsAvailableTime](   ptc.base,         q.getAvailablePartnerTimes.asScala.toMap, q)
    verify[PsClassicalTime](   ptc.classical,    q.getClassicalPartnerCharges.asScala.toMap, q)
    verify[PsRolloverPartnerCharge](ptc.rollover,q.getRolloverPartnerCharges.asScala.toMap, q)
  }

  def verify[T <: PsPartnerCharge](pt: PartnerTime, pcMap: Map[PsPartner, T], q: PsQueue) {
    partners.values foreach {
      p => {
        val time      = pt(p)
        val chargeOpt = pcMap.get(partners.psPartnerFor(p))

        if (time.isZero)
          PartnerTimeCalcPersisterTest.verifyNoTime(chargeOpt)
        else
          PartnerTimeCalcPersisterTest.verifyTimeMatches(time, chargeOpt.get)
      }
    }

    pcMap foreach {
      case (psPartner, charge) =>
        assertSame(q,         charge.getQueue)
        assertSame(psPartner, charge.getPartner)
    }
  }
}

object PartnerTimeCalcPersisterTest {
  def verifyNoTime(pcOpt: Option[PsPartnerCharge]) {
    pcOpt foreach {
      pc => verifyTimeMatches(Time.ZeroHours, pc)
    }
  }

  def verifyTimeMatches(t: Time, pc: PsPartnerCharge) {
    assertEquals(t.toHours.value, pc.getCharge.getDoubleValue, 0.000001)
  }
}