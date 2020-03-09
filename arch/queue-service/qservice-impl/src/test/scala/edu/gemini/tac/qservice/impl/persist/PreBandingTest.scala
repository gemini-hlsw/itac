package edu.gemini.tac.qservice.impl.persist

import org.junit._
import Assert._
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import edu.gemini.tac.qengine.api.queue.time.{PartnerTime, QueueTime}
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1.{Mode, Ntac, CoreProposal}
import edu.gemini.tac.qengine.ctx.{Semester, Context, Site}

class PreBandingTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  val site = Site.south
  val ctx = new Context(site, Semester.parse("2011B"))
  val qtime = QueueTime(site, PartnerTime.distribute(Time.hours(100), site, partners).map, partners)

  @Test def testEmpty() {
    PreBanding.toList(ctx, ProposalQueueBuilder(qtime)) match {
      case Nil => // ok
      case _ => fail()
    }
  }

  @Test def testOne() {
    val propUS = CoreProposal(
      Ntac(US, "us1", 1.0, Time.hours(10)), Site.south, piName = Some("Walker")
    )

    val q0 = ProposalQueueBuilder(qtime)
    val q1 = q0 :+ propUS

    PreBanding.toList(ctx, q1) match {
      case List(p0) =>
        assertEquals("Walker", p0.prop.piName.get)
        assertEquals(0, p0.pos.index)
        assertEquals(1, p0.pos.band.number)
        assertEquals("GS-2011B-Q-1", p0.progId.toString)
      case _ => fail()
    }
  }

  @Test def testSorting() {
    val propUS = CoreProposal(
      Ntac(US, "us1", 1.0, Time.hours(10)), Site.south, piName = Some("Walker")
    )
    val propGS = CoreProposal(
      Ntac(GS, "gs1", 1.0, Time.hours(10)), Site.south, piName = Some("Dawson")
    )
    val propAU = CoreProposal(
      Ntac(AU, "au1", 1.0, Time.hours(10)), Site.south, piName = Some("Nussberger")
    )
    val propCL = CoreProposal(
      Ntac(CL, "cl1", 1.0, Time.hours(10)), Site.south, piName = Some("Nunez")
    )
    val propBR = CoreProposal(
      Ntac(BR, "br1", 1.0, Time.hours(10)), Site.south, piName = Some("O'Brien")
    )

    val q0 = ProposalQueueBuilder(qtime)
    val q1 = q0 :+ propUS :+ propGS :+ propAU :+ propCL :+ propBR

    // Band 1 is 30 hours.
    // Sorts to Walker, Nussberger, Dawson in band 1, then O'Brien, Nunez in
    // band 2.

    PreBanding.toList(ctx, q1) match {
      case List(p0, p1, p2, p3, p4) =>
        assertEquals("Walker", p0.prop.piName.get)
        assertEquals(0, p0.pos.index)
        assertEquals(1, p0.pos.band.number)
        assertEquals("GS-2011B-Q-1", p0.progId.toString)

        assertEquals("Nussberger", p1.prop.piName.get)
        assertEquals(2, p1.pos.index)
        assertEquals(1, p1.pos.band.number)
        assertEquals("GS-2011B-Q-2", p1.progId.toString)

        assertEquals("Dawson", p2.prop.piName.get)
        assertEquals(1, p2.pos.index)
        assertEquals(1, p2.pos.band.number)
        assertEquals("GS-2011B-Q-3", p2.progId.toString)

        // switch to band 2
        assertEquals("O'Brien", p3.prop.piName.get)
        assertEquals(4, p3.pos.index)
        assertEquals(2, p3.pos.band.number)
        assertEquals("GS-2011B-Q-4", p3.progId.toString)

        assertEquals("Nunez", p4.prop.piName.get)
        assertEquals(3, p4.pos.index)
        assertEquals(2, p4.pos.band.number)
        assertEquals("GS-2011B-Q-5", p4.progId.toString)
      case _ => fail()
    }
  }

  @Test def testModeGrouping() {
    val propUS = CoreProposal(
      Ntac(US, "us1", 1.0, Time.hours(10)), Site.south, piName = Some("Walker")
    )
    val propGS = CoreProposal(
      Ntac(GS, "gs1", 1.0, Time.hours(10)), Site.south, piName = Some("Dawson"), mode = Mode.LargeProgram
    )
    val propAU = CoreProposal(
      Ntac(AU, "au1", 1.0, Time.hours(10)), Site.south, piName = Some("Nussberger")
    )
    val propCL = CoreProposal(
      Ntac(CL, "cl1", 1.0, Time.hours(10)), Site.south, piName = Some("Nunez")
    )
    val propBR = CoreProposal(
      Ntac(BR, "br1", 1.0, Time.hours(10)), Site.south, piName = Some("O'Brien")
    )

    val q0 = ProposalQueueBuilder(qtime)
    val q1 = q0 :+ propUS :+ propGS :+ propAU :+ propCL :+ propBR

    // Band 1 is 30 hours.
    // Sorts to Walker, Nussberger, Dawson in band 1, then O'Brien, Nunez in
    // band 2.
    // Then Dawson (LP) is put in front of the queue programs.

    PreBanding.toList(ctx, q1) match {
      case List(p0, p1, p2, p3, p4) =>
        assertEquals("Dawson", p0.prop.piName.get)
        assertEquals(1, p0.pos.index)
        assertEquals(1, p0.pos.band.number)
        assertEquals("GS-2011B-LP-1", p0.progId.toString)

        assertEquals("Walker", p1.prop.piName.get)
        assertEquals(0, p1.pos.index)
        assertEquals(1, p1.pos.band.number)
        assertEquals("GS-2011B-Q-1", p1.progId.toString)

        assertEquals("Nussberger", p2.prop.piName.get)
        assertEquals(2, p2.pos.index)
        assertEquals(1, p2.pos.band.number)
        assertEquals("GS-2011B-Q-2", p2.progId.toString)

        // switch to band 2
        assertEquals("O'Brien", p3.prop.piName.get)
        assertEquals(4, p3.pos.index)
        assertEquals(2, p3.pos.band.number)
        assertEquals("GS-2011B-Q-3", p3.progId.toString)

        assertEquals("Nunez", p4.prop.piName.get)
        assertEquals(3, p4.pos.index)
        assertEquals(2, p4.pos.band.number)
        assertEquals("GS-2011B-Q-4", p4.progId.toString)
      case _ => fail()
    }
  }

}