package edu.gemini.tac.qengine.impl.queue

import edu.gemini.tac.qengine.api.queue.ProposalPosition
import edu.gemini.tac.qengine.api.queue.time.{PartnerTime, QueueTime}
import edu.gemini.tac.qengine.ctx.{Partner, Site}
import edu.gemini.tac.qengine.p1.QueueBand._
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.util.Time

import org.junit._
import Assert._

/**
 *
 */
class FinalProposalQueueTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  val delta     = 0.000001
  val site      = Site.south
  val queueTime = new QueueTime(site, PartnerTime.distribute(Time.hours(100), site, partners))

  private def mkProp(partner: Partner, propTimeHours: Int, id: String): CoreProposal = {
    val ntac = Ntac(partner, id, 0, Time.hours(propTimeHours))

    // Make a proposal with just no observations.  We won't be using them anyway.
    CoreProposal(ntac, site = this.site)
  }


  @Test def testCompleteBands() {
    val queue = new FinalProposalQueue(queueTime, Map.empty)

    QueueBand.values foreach {
      band =>
        assertTrue(queue.bandedQueue.contains(band))
        assertEquals(Nil, queue.bandedQueue(band))
    }
  }

  private def verifyTime(expected: Double, actual: Time) {
    assertEquals(expected, actual.toHours.value, 0.000001)
  }

  @Test def testEmptyQueue() {
    val queue = new FinalProposalQueue(queueTime, Map.empty)

    assertEquals(Time.Zero, queue.usedTime)
    assertEquals(Nil, queue.toList)
    assertEquals(Nil, queue.zipWithPosition)
    assertTrue(queue.positionOf(mkProp(GS, 1, "GS-1")).isEmpty)

    verifyTime(US.percentAt(site), queue.remainingTime(US))
  }

  @Test def testSingleProposal() {
    val gs1     = mkProp(GS, 1, "GS-1")
    val bandMap = Map[QueueBand, List[Proposal]](QBand2 -> List(gs1))
    val queue   = new FinalProposalQueue(queueTime, bandMap)

    // Queue starts in band 2 because we constructed it that way.  Should work
    // anyway.
    val expectedPos = ProposalPosition(0, Time.Zero, QBand2, 0, Time.Zero)

    queue.zipWithPosition match {
      case (prop, pos) :: Nil =>
        assertEquals(gs1.id, prop.id)
        assertEquals(expectedPos, pos)
      case _ => fail()
    }

    assertEquals(List(gs1), queue.toList)

    assertEquals(Some(expectedPos), queue.positionOf(gs1))

    verifyTime(99.0, queue.remainingTime)
    verifyTime(GS.percentAt(site) - 1, queue.remainingTime(GS))
    verifyTime(US.percentAt(site), queue.remainingTime(US))
    verifyTime(queueTime(Category.B1_2).toHours.value - 1, queue.remainingTime(Category.B1_2))
    verifyTime(queueTime(Category.Guaranteed).toHours.value - 1, queue.remainingTime(Category.Guaranteed))
  }

  @Test def testMultipleProposals() {
    val au1     = mkProp(AU, 1, "AU-1")
    val gs3     = mkProp(GS, 3, "GS-3")
    val us4     = mkProp(US, 4, "US-4")
    val bandMap = Map[QueueBand, List[Proposal]](
      QBand1 -> List(au1),
      QBand3 -> List(gs3),
      QBand4 -> List(us4)
    )
    val queue   = new FinalProposalQueue(queueTime, bandMap)

    assertEquals(List(au1,gs3,us4), queue.toList)

    // Skip band 2 because we didn't add any proposals at band 2.
    val auPos = ProposalPosition(0, Time.Zero,     QBand1, 0, Time.Zero)
    val gsPos = ProposalPosition(1, Time.hours(1), QBand3, 0, Time.Zero)
    val usPos = ProposalPosition(2, Time.hours(4), QBand4, 0, Time.Zero)

    queue.zipWithPosition match {
      case (prop0, pos0) :: (prop1, pos1) :: (prop2, pos2) :: Nil =>
        assertEquals(au1.id, prop0.id)
        assertEquals(auPos,  pos0)
        assertEquals(gs3.id, prop1.id)
        assertEquals(gsPos,  pos1)
        assertEquals(us4.id, prop2.id)
        assertEquals(usPos,  pos2)
      case _ => fail()
    }

    assertEquals(Some(gsPos), queue.positionOf(gs3))

    verifyTime(92.0, queue.remainingTime)
    verifyTime(GS.percentAt(site) - 3, queue.remainingTime(GS))
    verifyTime(US.percentAt(site) - 4, queue.remainingTime(US))
    verifyTime(queueTime(Category.B1_2).toHours.value - 1, queue.remainingTime(Category.B1_2)) // 1 hr band 1,2
    verifyTime(queueTime(Category.Guaranteed).toHours.value - 4, queue.remainingTime(Category.Guaranteed)) // 4 hrs band 1,2,3
  }

  @Test def testMultipleProposalsInMultipleBands() {
    val au1     = mkProp(AU, 1, "AU-1")
    val ca1     = mkProp(CA, 1, "CA-1")
    val gs4     = mkProp(GS, 4, "GS-4")
    val us4     = mkProp(US, 4, "US-4")
    val bandMap = Map[QueueBand, List[Proposal]](
      QBand1 -> List(au1, ca1),
      QBand3 -> List(gs4, us4)
    )
    val queue   = new FinalProposalQueue(queueTime, bandMap)

    assertEquals(List(au1,ca1,gs4,us4), queue.toList)

    // Skip band 2 because we didn't add any proposals at band 2.
    val auPos = ProposalPosition(0, Time.Zero,     QBand1, 0, Time.Zero)
    val caPos = ProposalPosition(1, Time.hours(1), QBand1, 1, Time.hours(1))
    val gsPos = ProposalPosition(2, Time.hours(2), QBand3, 0, Time.Zero)
    val usPos = ProposalPosition(3, Time.hours(6), QBand3, 1, Time.hours(4))

    queue.zipWithPosition match {
      case (prop0, pos0) :: (prop1, pos1) :: (prop2, pos2) :: (prop3, pos3):: Nil =>
        assertEquals(au1.id, prop0.id)
        assertEquals(auPos,  pos0)
        assertEquals(ca1.id, prop1.id)
        assertEquals(caPos,  pos1)
        assertEquals(gs4.id, prop2.id)
        assertEquals(gsPos,  pos2)
        assertEquals(us4.id, prop3.id)
        assertEquals(usPos,  pos3)
      case _ => fail()
    }

    assertEquals(Some(gsPos), queue.positionOf(gs4))

    verifyTime(90.0, queue.remainingTime)
    verifyTime(AU.percentAt(site) - 1.0, queue.remainingTime(AU))
    verifyTime(CA.percentAt(site) - 1.0, queue.remainingTime(CA))
    verifyTime(GS.percentAt(site) - 4.0, queue.remainingTime(GS))
    verifyTime(US.percentAt(site) - 4.0, queue.remainingTime(US))
    verifyTime(queueTime(Category.B1_2).toHours.value - 2.0, queue.remainingTime(Category.B1_2))
    verifyTime(queueTime(Category.Guaranteed).toHours.value - 10.0, queue.remainingTime(Category.Guaranteed))
  }
}
