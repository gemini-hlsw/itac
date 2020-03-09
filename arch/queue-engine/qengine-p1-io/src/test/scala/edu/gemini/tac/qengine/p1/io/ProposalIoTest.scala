package edu.gemini.tac.qengine.p1.io

import edu.gemini.model.p1.{immutable => im}
import edu.gemini.model.p1.{mutable => m}
import edu.gemini.tac.qengine.ctx.{TestPartners, Site}
import edu.gemini.tac.qengine.p1._

import org.junit._
import Assert._

import scalaz._
import Scalaz._
import scala.collection.immutable.Nil
import edu.gemini.tac.qengine.util.Time

class ProposalIoTest {
  val propIo = new ProposalIo(TestPartners.AllMap)
  val when   = System.currentTimeMillis()

  @Test def testModeQueue(): Unit = {
    assertEquals(Mode.Queue, ProposalIo.mode((new ProposalFixture).proposal))
  }

  @Test def testModeClassical(): Unit = {
    val p = new ProposalFixture {
      override def proposalClass = im.ClassicalProposalClass(
        None,
        None,
        proposalKey,
        queueSubs,
        Nil
      )
    }
    assertEquals(Mode.Classical, ProposalIo.mode(p.proposal))
  }

  @Test def testModeLarge(): Unit = {
    val p = new ProposalFixture {
      override def proposalClass = im.LargeProgramClass(
        None,
        None,
        proposalKey,
        im.LargeProgramSubmission(submissionRequest, submissionResponse),
        tooOption
      )
    }
    assertEquals(Mode.LargeProgram, ProposalIo.mode(p.proposal))
  }

  @Test def testModeDefaultQueue(): Unit = {
    val p = new ProposalFixture {
      override def proposalClass = im.ExchangeProposalClass(
        None,
        None,
        proposalKey,
        im.ExchangePartner.SUBARU,
        List(ngoSubmission)
      )
    }
    assertEquals(Mode.Queue, ProposalIo.mode(p.proposal))
  }

  @Test def testTooNone(): Unit = {
    assertEquals(Too.none, ProposalIo.too((new ProposalFixture).proposal))
  }

  @Test def testTooForNonQueue(): Unit = {
    val p = new ProposalFixture {
      override def proposalClass = im.ClassicalProposalClass(
        None,
        None,
        proposalKey,
        queueSubs,
        Nil
      )
    }
    assertEquals(Too.none, ProposalIo.too(p.proposal))
  }

  @Test def testTooRapid(): Unit = {
    val p = new ProposalFixture {
      override def tooOption = m.TooOption.RAPID
    }
    assertEquals(Too.rapid, ProposalIo.too(p.proposal))
  }

  @Test def testPiName(): Unit = {
    assertTrue(ProposalIo.piName((new ProposalFixture).proposal).contains("Henderson"))
  }

  @Test def testSingleProposal(): Unit = {
    val idGen = JointIdGen(0)

    propIo.read((new ProposalFixture).proposal, when, idGen) match {
      case Success((NonEmptyList(prop, _), gen2)) =>
        assertEquals(1, gen2.next.count) // not advanced
        assertTrue(prop.piName.contains("Henderson"))
        assertEquals(Too.none, prop.too)
        assertEquals(Nil, prop.band3Observations)
        assertEquals(1, prop.obsList.size)
        assertEquals(Site.south, prop.site)
        assertEquals(TestPartners.AU, prop.ntac.partner)
        assertEquals(Time.hours(1.0), prop.ntac.awardedTime)
      case _ => fail()
    }
  }

  @Test def testJointProposal(): Unit = {
    import NtacIoTest._

    val sub2  = im.NgoSubmission(
      request2,
      response2,
      im.NgoPartner.US,
      im.InvestigatorRef.empty
    )

    val idGen = JointIdGen(0)

    val p = new ProposalFixture {
      override def queueSubs: Either[List[im.NgoSubmission], im.ExchangeSubmission] =
        Left(List(ngoSubmission, sub2))
    }

    propIo.read(p.proposal, when, idGen) match {
      case Success((NonEmptyList(prop, _), gen2)) =>
        assertEquals(2, gen2.next.count)
        assertTrue(prop.jointId.contains("j0"))
        prop match {
          case jp: JointProposal =>
            val ps = jp.toParts.map(_.ntac.partner).toSet
            assertEquals(Set(TestPartners.AU, TestPartners.US), ps)
          case _ => fail("expected au and gs partners")
        }
      case _ => fail()
    }
  }

  @Test def testNorthAndSouth(): Unit = {
    import ObservationIoTest.{when=>_, _}

    val idGen = JointIdGen(0)

    val p = new ProposalFixture {
      private def nifsObservation = im.Observation(
        Some(nifsAo),
        Some(conditions),
        Some(siderealTarget),
        im.Band.BAND_1_2,
        Some(oneHour)
      )
      override def observations = List(observation, nifsObservation)
    }

    propIo.read(p.proposal, when, idGen) match {
      case Success((NonEmptyList(prop0, prop1), gen2)) =>
        assertEquals(1, gen2.next.count)
        assertEquals(Set(Site.north, Site.south), Set(prop0.site, prop1.headOption.get.site))
      case _ => fail()
    }
  }
}
