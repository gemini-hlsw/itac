package edu.gemini.tac.qservice.impl.persist

import edu.gemini.tac.persistence.phase1.{TimeUnit => PsTimeUnit}
import edu.gemini.tac.persistence.queues.{Banding => PsBanding, JointBanding => PsJointBanding, Queue => PsQueue, ScienceBand => PsScienceBand}
import edu.gemini.tac.qengine.ctx.{Semester, Site}
import edu.gemini.tac.qengine.p1.{Mode, QueueBand}
import edu.gemini.tac.qengine.p2.ProgramId
import edu.gemini.tac.qservice.impl.p1.{PsFixture => Fix}

import org.junit._
import Assert._

import scala.collection.JavaConverters._


class PsBandingFactoryTest {
  val q = new PsQueue("testqueue", Fix.committee)

  val mappings = List(
    Fix.psJointAR1_BR1 -> Fix.jointAR1_BR1
    , Fix.psAR1 -> Fix.coreAR1
    , Fix.psBR1 -> Fix.coreBR1
  )
//  val m = PsProposalMap(mappings)

  val f = new PsBandingFactory(QueueBand.QBand2, mappings, q)

  val progId = ProgramId(Site.south, Semester.parse("2011B"), Mode.Queue, 3)

  @Test def testNormalProposalMakesNormalBanding() {

    f(Fix.coreAR1, 42, progId) match {
      case b: PsBanding =>
        assertEquals(PsScienceBand.BAND_TWO, b.getBand)
        assertSame(q, b.getQueue)
        assertSame(Fix.psAR1, b.getProposal)
        assertFalse(b.isJoint)
        assertFalse(b.isJointComponent)
        assertEquals(42, b.getMergeIndex)
        assertEquals("GS-2011B-Q-3", b.getProgramId)
        assertEquals(10.0, b.getAwardedTime.getDoubleValue, 0.000001)
        assertEquals(PsTimeUnit.HR, b.getAwardedTime.getUnits)
      case _ =>
        fail()
    }
  }

  @Test def testJointProposalMakesJointBanding() {
    /* This test seems to be in tension with JointProposalTest as to whether or not the
     core proposal's ntac is or is not automatically added to the ntacs of the
     qengine.p1.JointProposal

     JointProposalTest.testExpandJoints (which is at a lower level) explicitly tests
      that if the core proposal is not in the ntacs, then "expandJoints" does not
      include the core proposal.

      On the other hand, this test, as it was before being ported to the 2012 model,
      worked with a fixture in which the core was not explicitly set, but the tests
      seemed to expect that the core proposal's ntac values would be taken into account.

      */
    f(Fix.jointAR1_BR1, 42, progId) match {
      case b: PsJointBanding =>
        assertEquals(PsScienceBand.BAND_TWO, b.getBand)
        assertSame(q, b.getQueue)
        assertSame(Fix.psJointAR1_BR1, b.getProposal)
        assertTrue(b.isJoint)
        assertFalse(b.isJointComponent)
        assertEquals(42, b.getMergeIndex)
        assertEquals("GS-2011B-Q-3", b.getProgramId)
        assertEquals(20.0, b.getAwardedTime.getDoubleValue, 0.000001)
        assertEquals(PsTimeUnit.HR, b.getAwardedTime.getUnits)

        val expected = Set(Fix.psAR1, Fix.psBR1)
        val actual = b.getBandings.asScala.map(_.getProposal)
        assertEquals(expected, actual)
      case _ =>
        fail()
    }
  }
}