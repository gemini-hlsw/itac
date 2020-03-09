package edu.gemini.tac.qengine.impl

import block.BlockIterator
import org.junit._
import Assert._

import resource.Fixture
import edu.gemini.tac.qengine.api.config.Default
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.log._
import edu.gemini.tac.qengine.api.queue.ProposalQueue
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.ctx.Partner

class QueueFrameTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  private def remainingDecHours(f: QueueFrame, t: Target): Double =
    f.res.ra.grp(t).remaining(t).toHours.value

  private def remainingCondsHours(f: QueueFrame, t: Target, c: ObsConditions): Double =
    f.res.ra.grp(t).remaining(c).toHours.value

  private def semesterRes(queue: ProposalQueue) =
    Fixture.semesterRes(queue.queueTime(QueueBand.Category.Guaranteed))

  @Test def testEnqueueProposal() {
    val qstate = Fixture.evenQueue(10.0) // 10 hrs per partner
    val target20 = Target(15.0 * 20, 0.0) // 20 hrs, 0 deg

    // 30 minute proposal
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(0.5)),
      (target20, Fixture.badCC, Time.hours(0.5))
    )

    // Block iterator with just one block for the proposal/obs
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1))
    val blit = BlockIterator(partners, Fixture.genQuanta(1.0), List(US), propLists, _.obsList)
    val activeList: (Proposal) => List[Observation] = _.obsList

    // Create the frame
    val res = semesterRes(qstate)
    val frame1 = new QueueFrame(qstate, blit, res)

    // Produce the next frame
    val next2 = frame1.next(activeList).right.get
    val frame2 = next2.frame

    // Should enqueue the the proposal.
    assertEquals(propUS1, frame2.queue.toList.head)
    assertEquals(Nil, frame2.queue.toList.tail)

    next2.accept match {
      case Some(msg: AcceptMessage) => assertEquals(propUS1, msg.prop)
      case _ => fail()
    }

    // Should update the RA bin.
    assertEquals(19.5, remainingDecHours(frame2, target20), 0.000001)
    assertEquals(19.5, remainingCondsHours(frame2, target20, Fixture.badCC), 0.000001)

    // Shouldn't have any more frames
    assertFalse(frame2.hasNext)
  }

  @Test def testPartnerTimeOverAllocation() {
    val qstate = Fixture.evenQueue(1.0, None) // 1 hrs per partner
    val target20 = Target(15.0 * 20, 0.0) // 20 hrs, 0 deg

    // 3 45 minute proposals

    // propUS1 is scheduled because it starts and ends before the partner limit
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(0.75)),
      (target20, Fixture.badCC, Time.hours(0.75))
    )

    // propUS2 is scheduled because it starts before the partner limit (though
    // it takes more time than the limit permits).
    val propUS2 = Fixture.mkProp(Ntac(US, "US2", 2, Time.hours(0.75)),
      (target20, Fixture.badCC, Time.hours(0.75))
    )

    // propUS3 is not scheduled because it starts after the partner limit is
    // reached
    val propUS3 = Fixture.mkProp(Ntac(US, "US3", 3, Time.hours(0.75)),
      (target20, Fixture.badCC, Time.hours(0.75))
    )

    // Block iterator with block to cover the obs in each proposal
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1, propUS2, propUS3))
    val blit = BlockIterator(partners, Fixture.genQuanta(1.0), List(US, US, US, US), propLists, _.obsList)

    // Create the frame
    val res = semesterRes(qstate)
    val frame1 = new QueueFrame(qstate, blit, res)

    // Schedule the first two proposals.  The first proposal goes in the first
    // block.  The second proposal is split over two blocks because the time
    // quanta is 1 hour.  So, 3 blocks to schedule the two proposals
    val next2 = frame1.next(_.obsList).right.get
    val frame2 = next2.frame
    next2.accept match {
      case Some(msg: AcceptMessage) => assertEquals(propUS1, msg.prop)
      case _ => fail()
    }

    val next3 = frame2.next(_.obsList).right.get
    val frame3 = next3.frame
    next3.accept match {
      case None => // ok nothing accepted or rejected here
      case _ => fail()
    }

    val next4 = frame3.next(_.obsList).right.get
    val frame4 = next4.frame
    next4.accept match {
      case Some(msg: AcceptMessage) => assertEquals(propUS2, msg.prop)
      case _ => fail()
    }

    // Should enqueue the the two proposals.
    assertEquals(propUS1, frame4.queue.toList.head)
    assertEquals(propUS2, frame4.queue.toList.tail.head)
    assertEquals(Nil, frame4.queue.toList.tail.tail)

    // Should update the RA bin with 1.5 hours used
    assertEquals(18.5, remainingDecHours(frame4, target20), 0.000001)
    assertEquals(18.5, remainingCondsHours(frame4, target20, Fixture.badCC), 0.000001)

    // Now try to add the last proposal.  It will be rejected because the
    // partner has already used its allocation.
    frame4.next(_.obsList) match {
      case Left(msg: RejectPartnerOverAllocation) => assertEquals(propUS3, msg.prop)
      case _ => fail()
    }
  }

  // Reject a longer proposal because it requires too much time, but accept
  // a shorter subsequent proposal.
  @Test def testOverAllocation() {
    val qstate = Fixture.evenQueue(1.0, None) // 1 hrs per partner
    val target20 = Target(15.0 * 20, 0.0) // 20 hrs, 0 deg

    // 3 45 minute proposals

    // propUS1 is scheduled because it starts and ends before the partner limit
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(0.75)),
      (target20, Fixture.badCC, Time.hours(0.75))
    )

    // propUS2 is not scheduled because it would use more than the remaining
    // queue time.
    // 1 hour per partner in the queue, 0.75 hours used, + a bit to take it over
    // the limit
    val hrs = (1.0 * partners.length) - 0.75 + 0.0001
    val propUS2 = Fixture.mkProp(Ntac(US, "US2", 2, Time.hours(hrs)),
      (target20, Fixture.badCC, Time.hours(hrs))
    )

    // propUS3 is scheduled even though it goes over the partner limit because
    // it starts before the partner limit is reached.
    val propUS3 = Fixture.mkProp(Ntac(US, "US3", 3, Time.hours(0.75)),
      (target20, Fixture.badCC, Time.hours(0.75))
    )

    // Block iterator with block to cover the obs in each proposal
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1, propUS2, propUS3))
    val blit = BlockIterator(partners, Fixture.genQuanta(1.0), List(US, US), propLists, _.obsList)

    // Create the frame
    val res = semesterRes(qstate)
    val frame1 = new QueueFrame(qstate, blit, res)

    // Schedule the first proposal.
    val frame2 = frame1.next(_.obsList).right.get.frame
    assertEquals(propUS1, frame2.queue.toList.head)

    // 15 minutes left in the first time quanta.
    // The next proposal should get rejected right away.
    // Now try to add the last proposal.  It will be rejected because the
    // partner has already used its allocation.
    frame2.next(_.obsList) match {
      case Left(msg: RejectOverAllocation) => assertEquals(propUS2, msg.prop)
      case _ => fail()
    }

    // Needs 15 minutes of the first quanta and 30 of the second to schedule
    // US3
    val frame3 = frame2.skip(_.obsList).next(_.obsList).right.get.frame.next(_.obsList).right.get.frame
    assertEquals(propUS3, frame3.queue.toList.last)
  }

  @Test def testSkip() {
    val qstate = Fixture.evenQueue(1.0, None) // 1 hrs per partner
    val target20 = Target(15.0 * 20, 0.0) // 20 hrs, 0 deg

    // 3 45 minute proposals

    // propUS1 is scheduled because it starts and ends before the partner limit
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(0.75)),
      (target20, Fixture.badCC, Time.hours(0.75))
    )

    // propUS2 is skipped
    val propUS2 = Fixture.mkProp(Ntac(US, "US2", 2, Time.hours(0.75)),
      (target20, Fixture.badCC, Time.hours(0.75))
    )

    // propUS3 is scheduled because it starts before the partner limit (though
    // it takes more time than the limit permits).
    val propUS3 = Fixture.mkProp(Ntac(US, "US3", 3, Time.hours(0.75)),
      (target20, Fixture.badCC, Time.hours(0.75))
    )

    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1, propUS2, propUS3))
    val blit = BlockIterator(partners, Fixture.genQuanta(1.0), List(US, US, US), propLists, _.obsList)

    // Create the frame
    val res = semesterRes(qstate)
    val frame1 = new QueueFrame(qstate, blit, res)

    // We're at the beginning of the first proposal.
    assertTrue(frame1.isStartOf(propUS1))
    assertFalse(frame1.isStartOf(propUS2))

    val frame2 = frame1.next(_.obsList).right.get.frame

    // Should enqueue the first proposal.
    assertEquals(propUS1, frame2.queue.toList.head)

    // We're at the beginning of the second proposal.
    assertTrue(frame2.isStartOf(propUS2))

    // Skip it.
    val frame3 = frame2.skip(_.obsList)

    // Now we're at the start of prop3
    assertTrue(frame3.isStartOf(propUS3))

    // Still has the first proposal in its queue.
    assertEquals(propUS1, frame3.queue.toList.head)

    // Still has the time for the first proposal recorded in its bins.
    assertEquals(19.25, remainingDecHours(frame3, target20), 0.000001)

    // Enqueue the last proposal.  15 minutes remaining in the first quantum,
    // 30 minutes in the second quantum.  So requires cycling through two steps
    val frame4 = frame3.next(_.obsList).right.get.frame.next(_.obsList).right.get.frame

    assertEquals(List(propUS1, propUS3), frame4.queue.toList)
    assertEquals(18.5, remainingDecHours(frame4, target20), 0.000001)
  }

  @Test def testRejectTarget() {
    val qstate = Fixture.evenQueue(2.0) // 2 hrs per partner
    val target20 = Target(15.0 * 1, 45.0) // 1 hrs, 45 deg

    // propUS1 is not scheduled because the RA bin for 1 hr contains only
    // 30 min in dec 45-90
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(0.75)),
      (target20, Fixture.badCC, Time.hours(0.75))
    )

    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1))
    val blit = BlockIterator(partners, Fixture.genQuanta(1.0), List(US), propLists, _.obsList)

    // Create the frame
    val res = semesterRes(qstate)
    val frame1 = new QueueFrame(qstate, blit, res)

    // Fail because the target cannot be accommodated.
    frame1.next(_.obsList) match {
      case Left(msg: RejectTarget) => assertEquals(propUS1, msg.prop)
      case _ => fail()
    }
  }

  @Test def testToo() {
    val qstate = Fixture.evenQueue(23.0, None) // 23 hrs per partner
    val target0 = Target(0.0, 0.0) // not considered for ToO

    // TooBlocksTest already handles testing the split of time across blocks.
    // Here we just want to verify that it is applied.
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(23.0)),
      (target0, Fixture.badCC, Time.hours(23.0))
    ).copy(too = Too.standard)

    // Block iterator with just one block for the proposal/obs
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1))
    val blit = BlockIterator(partners, Fixture.genQuanta(23.0), List(US), propLists, _.obsList)

    // Create the frame
    val res = semesterRes(qstate)
    new QueueFrame(qstate, blit, res).next(_.obsList) match {
      case Right(next) => {
        // Should enqueue the the proposal.
        assertEquals(propUS1, next.frame.queue.toList.head)
        assertEquals(Nil, next.frame.queue.toList.tail)

        // Should update the RA bins.  First bin has 0 hr limit so it won't be
        // updated.
        val ra0 = next.frame.res.ra.grp.bins.head
        assertEquals(Time.Zero, ra0.remaining)

        next.frame.res.ra.grp.bins.tail.foreach(ra => assertEquals(ra.limit - Time.hours(1), ra.remaining))
      }
      case _ => fail()
    }
  }

  @Test def testTooReject() {
    val qstate = Fixture.evenQueue(277.0, None)
    val target0 = Target(0.0, 0.0) // not considered for ToO

    // TooBlocksTest already handles testing the split of time across blocks.
    // Here we just want to verify that it is applied.
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(277.0)),
      (target0, Fixture.badCC, Time.hours(277.0))
    ).copy(too = Too.standard)

    // Block iterator with just one block for the proposal/obs
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1))
    val blit = BlockIterator(partners, Fixture.genQuanta(277.0), List(US), propLists, _.obsList)

    // Create the frame
    val res = semesterRes(qstate)
    new QueueFrame(qstate, blit, res).next(_.obsList) match {
      case Left(msg: RejectToo) => assertEquals(propUS1, msg.prop)
      case _ => fail()
    }
  }

  @Test def testRestrictedRecord() {
    val qstate = Fixture.evenQueue(10.0) // 10 hrs per partner
    val target20 = Target(15.0 * 20, 0.0) // 20 hrs, 0 deg

    // LGS observation with good WV
    val conds = ObsConditions(CloudCover.CCAny, ImageQuality.IQAny, SkyBackground.SBAny, WaterVapor.WV20)
    val obs20 = Observation(target20, conds, Time.hours(10), true)

    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(0.5))).copy(
      obsList = List(obs20)
    )

    // Block iterator with just one block for the proposal/obs
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1))
    val blit = BlockIterator(partners, Fixture.genQuanta(1.0), List(US), propLists, _.obsList)

    // Create the frame
    val res = Fixture.semesterRes(Time.hours(200))
    val frame1 = new QueueFrame(qstate, blit, res)

    frame1.next(_.obsList) match {
      case Right(next) => {
        val restrictedGrp = next.frame.res.time
        val resWv = restrictedGrp.lst.head
        val resLgs = restrictedGrp.lst.tail.head

        assertEquals(Time.hours(99.5), resWv.remaining) // 200 * 50% - 0.5
        assertEquals(Time.hours(199.5), resLgs.remaining) // 200 - 0.5
      }
      case _ => fail()
    }
  }

  @Test def testWvReject() {
    val qstate = Fixture.evenQueue(10.0) // 10 hrs per partner
    val target20 = Target(15.0 * 20, 0.0) // 20 hrs, 0 deg

    val conds = ObsConditions(CloudCover.CCAny, ImageQuality.IQAny, SkyBackground.SBAny, WaterVapor.WV20)

    // 30 minute proposal
    val obs20 = Observation(target20, conds, Time.hours(10))
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(0.5))).copy(
      obsList = List(obs20)
    )

    // Block iterator with just one block for the proposal/obs
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1))
    val blit = BlockIterator(partners, Fixture.genQuanta(1.0), List(US), propLists, _.obsList)

    // Create the frame
    val res = Fixture.semesterRes(Time.minutes(1)) // 50% of 1 minute for good WV
    val frame1 = new QueueFrame(qstate, blit, res)

    frame1.next(_.obsList) match {
      case Left(msg: RejectRestrictedBin) =>
        assertTrue(msg.reason.contains(Default.WvTimeRestriction.name))
      case _ => fail()
    }
  }

  @Test def testLgsReject() {
    val qstate = Fixture.evenQueue(10.0) // 10 hrs per partner
    val target20 = Target(15.0 * 20, 0.0) // 20 hrs, 0 deg

    // 30 minute proposal
    val obslgs = Observation(target20, ObsConditions.AnyConditions, Time.hours(10), true) // LGS
    val propUS1 = Fixture.mkProp(Ntac(US, "US1", 1, Time.hours(0.5))).copy(
      obsList = List(obslgs)
    )

    // Block iterator with just one block for the proposal/obs
    val propLists: Map[Partner, List[Proposal]] = Map(US -> List(propUS1))
    val blit = BlockIterator(partners, Fixture.genQuanta(1.0), List(US), propLists, _.obsList)

    // Create the frame
    val res = Fixture.semesterRes(Time.minutes(1)) // 1 minute for LGS
    val frame1 = new QueueFrame(qstate, blit, res)

    frame1.next(_.obsList) match {
      case Left(msg: RejectRestrictedBin) =>
        assertTrue(msg.reason.contains(Default.LgsTimeRestriction.name))
      case _ => fail()
    }
  }

}
