package edu.gemini.tac.qengine.impl

import block.BlockIterator
import org.junit._
import Assert._

import edu.gemini.tac.qengine.p1.QueueBand.Category.{B1_2, B3}
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.QueueBand.Category.Guaranteed
import edu.gemini.tac.qengine.log._
import resource.Fixture
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.ctx.Partner

class QueueCalcTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._

  private def remainingDecHours(qc: QueueCalcStage, t: Target): Double =
    qc.resource.ra.grp(t).remaining(t).toHours.value

  private def remainingCondsHours(qc: QueueCalcStage, t: Target, c: ObservingConditions): Double =
    qc.resource.ra.grp(t).remaining(c).toHours.value


  @Test def testQueueUpdate() {
    val qstate = Fixture.evenQueue(10.0) // 10 hrs per partner
    val target20 = Target(15.0 * 20, 0.0) // 20 hrs, 0 deg

    // 30 minute proposal
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(0.5)),
      (target20, Fixture.badCC, Time.hours(0.5))
    )

    // Block iterator with just one block for the proposal/obs
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1))
    val blit = BlockIterator(All, Fixture.genQuanta(1.0), List(US), propLists, _.obsList)

    // Compute the queue
    val res = Fixture.semesterRes(qstate.queueTime(Guaranteed))
    val params = new QueueCalcStage.Params(B1_2, qstate, blit, _.obsList, res, ProposalLog.Empty)
    val calc = QueueCalcStage(params)

    // Should enqueue the the proposal.
    assertEquals(propUS1, calc.queue.toList.head)
    assertEquals(Nil, calc.queue.toList.tail)

    // Should update the RA bin.
    assertEquals(19.5, remainingDecHours(calc, target20), 0.000001)
    assertEquals(19.5, remainingCondsHours(calc, target20, Fixture.badCC), 0.000001)
  }

  @Test def testLogUpdate() {
    val qstate = Fixture.evenQueue(10.0) // 10 hrs per partner
    val target20 = Target(15.0 * 20, 0.0) // 20 hrs, 0 deg

    // 30 minute proposal
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(0.5)),
      (target20, Fixture.badCC, Time.hours(0.5))
    )

    // 30 minute proposal for RA 0 hrs (which has zero time)
    val target0 = Target(0.0, 0.0)
    val propUS2 = Fixture.mkProp(Ntac(US, "US2", 2, Time.hours(0.5)),
      (target0, Fixture.badCC, Time.hours(0.5))
    )

    // Block iterator that is long enough to schedule propUS1 and rejected propUS2
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1, propUS2))
    val blit = BlockIterator(All, Fixture.genQuanta(1.0), List(US, US), propLists, _.obsList)

    // Compute the queue
    val res = Fixture.semesterRes(qstate.queueTime(Guaranteed))
    val params = new QueueCalcStage.Params(B1_2, qstate, blit, _.obsList, res, ProposalLog.Empty)
    val calc = QueueCalcStage(params)

    // Should enqueue just the first proposal
    assertEquals(List(propUS1), calc.queue.toList)

    // Should rejected prop2 for its RA
    calc.log(propUS2.id, B1_2) match {
      case msg: RejectTarget => // ok
      case _ => fail()
    }
  }

  @Test def testStopWhenChangeTimeCategory() {
    val qstate = Fixture.evenQueue(1.0, None) // 1 hrs per partner
    val target23 = Target(15.0 * 23, 0.0) // 23 hrs, 0 deg

    // We have a total queue time Partner.values.length hours.  With default
    // band percentages, the "band1,2" category runs out after 60% is scheduled.
    val hrs = All.length.toDouble
    val b3 = hrs * 0.6

    // Create a long proposal that takes up almost all of band 1 and 2
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(b3 - 0.1)),
      (target23, Fixture.badCC, Time.hours(b3 - 0))
    )

    // Create a short proposal that will take us into band 3
    val propBR1 = Fixture.mkProp(Ntac(BR, "BR1", 1, Time.hours(0.2)),
      (target23, Fixture.badCC, Time.hours(0.2))
    )

    // Create a short proposal that would be scheduled in band 3, but of course
    // queue calc will stop before it gets there.
    val propGS1 = Fixture.mkProp(Ntac(GS, "GS1", 1, Time.hours(0.5)),
      (target23, Fixture.badCC, Time.hours(0.2))
    ).copy(band3Observations = List(new Observation(target23, ObservingConditions.AnyConditions, Time.hours(1))))

    // Create a proposal to take us past band 3.
    val propCA1 = Fixture.mkProp(Ntac(CA, "CA1", 1, Time.hours(qstate.queueTime.bandPercentages.band3 * hrs)),
      (target23, Fixture.badCC, Time.hours(0.2))
    ).copy(band3Observations = List(new Observation(target23, ObservingConditions.AnyConditions, Time.hours(1))))

    // Create a proposal that can't be scheduled because it goes in band 4.
    val propAU1 = Fixture.mkProp(Ntac(AU, "AU1", 1, Time.hours(0.2)),
      (target23, Fixture.badCC, Time.hours(0.2))
    ).copy(band3Observations = List(new Observation(target23, ObservingConditions.AnyConditions, Time.hours(1))))

    // Block iterator with a huge time quanta so that all of propUS1 can be
    // scheduled in one block.  We shouldn't get to GS.
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1), BR -> List(propBR1), GS -> List(propGS1), CA -> List(propCA1), AU -> List(propAU1))
    val blit = BlockIterator(All, Fixture.genQuanta(20.0), List(US, BR, GS, CA, AU), propLists, _.obsList)

    // Compute the queue
    val res = Fixture.semesterRes(qstate.queueTime(Guaranteed))
    val params = new QueueCalcStage.Params(B1_2, qstate, blit, _.obsList, res, ProposalLog.Empty)
    val calc = QueueCalcStage(params)

    // Should enqueue just the first two proposals
    assertEquals(List(propUS1, propBR1), calc.queue.toList)

    // Should rejected GS prop because it falls in band 3
    calc.log(propGS1.id, B1_2) match {
      case msg: RejectCategoryOverAllocation => // ok
        assertEquals(B1_2, msg.cat)
      case _ => fail()
    }

    // 23 hours total - (time for propUS1 + time for propBR1)
    assertEquals(23.0 - ((b3 - 0.1) + 0.2), remainingDecHours(calc, target23), 0.00001)

    // Now make a calc for Band3 phase.
    val params3 = new QueueCalcStage.Params(B3, calc.queue, calc.iter, _.band3Observations, calc.resource, calc.log)
    val calc3 = QueueCalcStage(params3)
    assertEquals(List(propUS1, propBR1, propGS1, propCA1), calc3.queue.toList)

    calc3.queue.bandedQueue.keys.map {
      band =>
        val ps = calc3.queue.bandedQueue.get(band).get
        println("Band %s has %d".format(band, ps.size))
    }
    //Confirm that it scheduled in Band3
    val b3Keys = calc3.queue.bandedQueue.keys.filter(_.isIn(B3))
    assertEquals(1, b3Keys.size)
    assertEquals(2, calc3.queue.bandedQueue.get(b3Keys.head).get.size)

    // Should rejected AU prop because it falls in band 4
    calc3.log(propAU1.id, B3) match {
      case msg: RejectCategoryOverAllocation => // ok
        assertEquals(B3, msg.cat)
      case _ => fail()
    }
  }

  @Test def testRollback() {
    val qstate = Fixture.evenQueue(10.0) // 10 hrs per partner
    val target10 = Target(15.0 * 10, 0.0) // 10 hrs, 0 deg

    // Create a long proposal that takes up half of the time for ra 10
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(5.0)),
      (target10, Fixture.badCC, Time.hours(5.0))
    )

    // Create a long proposal that cannot be scheduled at ra 10 after US1.
    // It will be split into two blocks.
    val propBR1 = Fixture.mkProp(Ntac(BR, "BR1", 1, Time.hours(5.1)),
      (target10, Fixture.badCC, Time.hours(5.1))
    )

    // Create a short proposal that will fit after US1, but only after rolling
    // back BR1
    val propGS1 = Fixture.mkProp(Ntac(GS, "GS1", 1, Time.hours(1.0)),
      (target10, Fixture.badCC, Time.hours(1.0))
    )

    // Block iterator with a 5hr time quanta so that all of propUS1 can be
    // scheduled in one block.
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1), BR -> List(propBR1), GS -> List(propGS1))
    val blit = BlockIterator(All, Fixture.genQuanta(5.0), List(US, BR, GS, BR), propLists, _.obsList)

    // Compute the queue
    val res = Fixture.semesterRes(qstate.queueTime(Guaranteed))
    val params = new QueueCalcStage.Params(B1_2, qstate, blit, _.obsList, res, ProposalLog.Empty)
    val calc = QueueCalcStage(params)

    // Should enqueue US1 and GS1.  BR1 had to be rolled back and GS1 redone to
    // make the queue.
    assertEquals(List(propUS1, propGS1), calc.queue.toList)

    // Should rejected BR prop because it was too long
    calc.log(propBR1.id, B1_2) match {
      case msg: RejectTarget => // ok
      case _ => fail()
    }

    // 10 hrs total - 5 for US1 - 1 for GS1
    assertEquals(4.0, remainingDecHours(calc, target10), 0.00001)

    // GS1 was initially rejected, but was redone
    calc.log(propGS1.id, B1_2) match {
      case msg: AcceptMessage => // ok
      case _ => fail()
    }
  }
}
