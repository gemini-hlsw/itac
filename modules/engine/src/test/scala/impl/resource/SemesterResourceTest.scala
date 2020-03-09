package edu.gemini.tac.qengine.impl.resource

import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.Time
import Fixture.{badCC, goodCC}
import edu.gemini.tac.qengine.impl.block.Block
import edu.gemini.tac.qengine.log.{RejectOverAllocation, RejectPartnerOverAllocation}
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.QueueBand.Category.Guaranteed

class SemesterResourceTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  @Test def testReserveAvailableUpdatesRaResourceGroup() {
    val sem1        = Fixture.semesterRes(Time.hours(100))
    val target      = Target(15.0, 0.0) // hr 1
    val (sem2, rem) = sem1.reserveAvailable(Time.minutes(31), target, goodCC)
    assertEquals(1.0, rem.toMinutes.value, 0.000001)

    val raRes = sem2.ra.grp(target)
    assertTrue(raRes.isFull(goodCC))
    assertTrue(raRes.isFull(target, goodCC))
    assertFalse(raRes.isFull)
    assertFalse(raRes.isFull(target))

    assertEquals(29.0, raRes.remaining(target).toMinutes.value, 0.000001)
    assertEquals(29.0, raRes.remaining.toMinutes.value, 0.000001)
    assertEquals(29.0, raRes.remaining(badCC).toMinutes.value, 0.000001)

    // Nothing else changes
    assertSame(sem1.band, sem2.band)
    assertSame(sem1.time, sem2.time)

    // Get all ra bins except the one we updated.
    val raBins = sem2.ra.grp.bins.zipWithIndex.filterNot {
      case (bin, ra) => ra == 1
    }
    // Should all have an unchanged remaining time.
    raBins.foreach {
      case (bin, ra) => assertEquals(ra, bin.remaining.toHours.value, 0.000001)
    }
  }

  private val ntac = Ntac(GS, "x", 0, Time.hours(1.0))

  private def mkProp(target: Target, conds: ObservingConditions): Proposal =
    Fixture.mkProp(ntac, (target, conds, Time.hours(1.0)))
  @Test def testPartnerAllocationLimitRespected() {
    val sem1 = Fixture.semesterRes(Time.hours(100))
    val q    = Fixture.evenQueue(1.25) // 1 hour of schedulable time per partner

    // Make a proposal that falls in RA 23 and uses badCC.  In other words,
    // one that maps to bins with a lot of time in the fixture.  Uses an hour
    // of queue time, leaving the partner with no time left.
    val target = Target(359.0, 0.0)
    val ntac1  = Ntac(GS, "x", 0, Time.hours(1.0))
    val prop1  = Fixture.mkProp(ntac1, (target, badCC, Time.hours(1.0)))
    val q2     = q :+ prop1

    // Now try to add a block for this partner
    val ntac2 = Ntac(GS, "x", 0, Time.millisecs(1))
    val prop2 = Fixture.mkProp(ntac2, (target, badCC, Time.millisecs(1)))
    val block = Block(prop2, prop2.obsList.head, Time.millisecs(1), isStart = true, isFinal = true)
    sem1.reserve(block, q2) match {
      case Left(RejectPartnerOverAllocation(`prop2`, guaranteed, all)) => // ok
        assertTrue(guaranteed.isFull) // no time remaining
      case Left(msg) => fail(msg.reason + ": " + msg.detail)
      case _         => fail()
    }
  }

  @Test def testPartnerOverallocationWithinLimit() {
    val sem1 = Fixture.semesterRes(Time.hours(100))
    val q    = Fixture.evenQueue(1.25) // 1 hour of schedulable time per partner

    val twoPerc = Time.hours(1.25 * 0.02)

    // Make a proposal that falls in RA 23 and uses badCC.  In other words,
    // one that maps to bins with a lot of time in the fixture.  Uses just under
    // 1 hour of queue time.
    val target = Target(359.0, 0.0)
    val ntac1  = Ntac(GS, "x", 0, Time.hours(1.0) - Time.seconds(1.0))
    val prop1  = Fixture.mkProp(ntac1, (target, badCC, Time.hours(1.0)))
    val q2     = q :+ prop1

    // Now try to add a block for this partner.  It goes over the 80% limit
    // but just within the 2% allowance.  So it should be accepted.
    val ntac2 = Ntac(GS, "x", 0, twoPerc)
    val prop2 = Fixture.mkProp(ntac2, (target, badCC, twoPerc))
    val block = Block(prop2, prop2.obsList.head, twoPerc, isStart = true, isFinal = true)
    sem1.reserve(block, q2) match {
      case Right(sem2) => // ok
      case Left(msg)   => fail(msg.reason + ": " + msg.detail)
      case _           => fail()
    }
  }

  @Test def testPartnerOverallocationJustOverLimit() {
    val sem1 = Fixture.semesterRes(Time.hours(100))
    val q    = Fixture.evenQueue(1.25) // 1 hour of schedulable time per partner

    val fivePerc = Time.hours(1.25 * 0.05)

    // Make a proposal that falls in RA 23 and uses badCC.  In other words,
    // one that maps to bins with a lot of time in the fixture.  Uses just under
    // 1 hour of queue time.
    val target = Target(359.0, 0.0)
    val ntac1  = Ntac(GS, "x", 0, Time.hours(1.0) - Time.seconds(1.0))
    val prop1  = Fixture.mkProp(ntac1, (target, badCC, Time.hours(1.0)))
    val q2     = q :+ prop1

    // Now try to add a block for this partner.  It goes over the 80% limit and
    // the 2% allowance (just barely).  It should get rejected.
    val ntac2 = Ntac(GS, "x", 0, fivePerc + Time.seconds(1.0))
    val prop2 = Fixture.mkProp(ntac2, (target, badCC, fivePerc))
    val block =
      Block(prop2, prop2.obsList.head, fivePerc + Time.seconds(1.0), isStart = true, isFinal = true)
    sem1.reserve(block, q2) match {
      case Left(RejectPartnerOverAllocation(`prop2`, guaranteed, all)) => // ok
        assertFalse(guaranteed.isFull) // time remaining, just not enough
      case Left(msg) => fail(msg.reason + ": " + msg.detail)
      case _         => fail()
    }
  }

  @Test def testQueueTooFull() {
    val sem1 = Fixture.semesterRes(Time.hours(100))
    val q    = Fixture.evenQueue(1.25) // 1 hour per partner

    // Make a giant proposal and add it to the queue.  It uses up almost all the
    // queue time.
    val ntacUS = Ntac(US, "us1", 0, q.queueTime(Guaranteed) - Time.minutes(1.0))
    val big    = Fixture.mkProp(ntacUS, (Target(359.0, 0.0), badCC, Time.hours(1.0)))
    val q2     = q :+ big
    assertEquals(QueueBand.QBand3, q2.band)

    // Make a tiny proposal for BR.  This takes us over the Band 3 limit but
    // under the full queue time.
    val ntacBR  = Ntac(BR, "br1", 0, Time.minutes(2.0))
    val obsDefs = (Target(359.0, 0.0), badCC, Time.hours(1.0))
    val b3s     = List(Observation(obsDefs._1, obsDefs._2, obsDefs._3))
    val small   = Fixture.mkProp(ntacBR, obsDefs).copy(band3Observations = b3s)
    val q3      = q2 :+ small
    assertEquals(QueueBand.QBand4, q3.band)

    // Make a modest proposal for GS. Partner has plenty of time but the queue
    // is too full.
    val prop  = mkProp(Target(359.0, 0.0), badCC)
    val block = Block(prop, prop.obsList.head, Time.minutes(2.0))
    sem1.reserve(block, q3) match {
      case Left(msg: RejectOverAllocation) =>
        assertEquals(RejectOverAllocation.noRemaining, msg.detail)
      case _ => fail()
    }

  }

  @Test def testCantGoPast100Percent() {
    val sem1 = Fixture.semesterRes(Time.hours(100))
    val q    = Fixture.evenQueue(1.25) // 1 hour per partner

    // Make a giant proposal and add it to the queue.  It uses up almost all the
    // queue time.
    val ntacUS = Ntac(US, "us1", 0, q.queueTime(Guaranteed) - Time.minutes(1.0))
    val bigUS  = Fixture.mkProp(ntacUS, (Target(359.0, 0.0), badCC, Time.hours(1.0)))
    val q2     = q :+ bigUS
    assertEquals(QueueBand.QBand3, q2.band)

    // Make a big proposal for BR.  This would take us over 100% of the queue.
    val ntacBR = Ntac(BR, "br1", 0, ntacUS.awardedTime)
    val bigBR = Fixture
      .mkProp(ntacBR, (Target(359.0, 0.0), badCC, Time.hours(1.0)))
      .copy(band3Observations = List.empty)
    val block = Block(bigBR, bigBR.obsList.head, Time.minutes(2.0))
    sem1.reserve(block, q2) match {
      case Left(msg: RejectOverAllocation) =>
        assertEquals(RejectOverAllocation.tooBig(bigBR.time, q2.remainingTime), msg.detail)
      case _ => fail()
    }
  }
}
