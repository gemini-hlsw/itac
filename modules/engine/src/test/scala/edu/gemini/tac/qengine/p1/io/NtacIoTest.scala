package edu.gemini.tac.qengine.p1.io

import edu.gemini.model.p1.{immutable => im}

import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.Site
import edu.gemini.tac.qengine.p1.Ntac
import edu.gemini.tac.qengine.util.Time

import org.junit._
import Assert._

import java.util.UUID

import scalaz._
import Scalaz._

object NtacIoTest {
  def hrs(h: Double): im.TimeAmount = im.TimeAmount(h, im.TimeUnit.HR)

  val request2  = im.SubmissionRequest(hrs(2), hrs(2), None, None)

  val decision2 = some(im.SubmissionDecision(Right(im.SubmissionAccept(
      "def@456.edu",
      10.0,
      hrs(2),
      hrs(2),
      poorWeather = false
  ))))

  val response2 = some(im.SubmissionResponse(
    im.SubmissionReceipt("zzz-123", System.currentTimeMillis(), None),
    decision2,
    None
  ))
}

import NtacIoTest._

class NtacIoTest {
  import Partner._

  val ntacIo = new NtacIo

  // HACK HACK HACK
  // Let's ignore the p1 and NGO email nodes in Ntac (added long after this test was written) because they're not relevant.
  def clean(n: Ntac): Ntac = n.copy(submission = null, ngoEmail = None)
  def assertEquals(a: Any, b: Any): Unit = Assert.assertEquals(a, b)
  def assertEquals(n1: Ntac, n2: Ntac): Unit = Assert.assertEquals(clean(n1), clean(n2))
  def assertEquals(ns1: IList[Ntac], ns2: IList[Ntac]): Unit = Assert.assertEquals(ns1.map(clean), ns2.map(clean))

  def fixtureNtac(p: ProposalFixture) = {
    Ntac(CL, p.submissionId, Ntac.Rank(some(p.partnerRanking)), Time.hours(1.0), poorWeather = false, some(p.pi.lastName))
  }

  @Test def testNonJointQueue() {
    val p = new ProposalFixture
    ntacIo.read(p.proposal) match {
      case Success(NonEmptyList(ntac, _)) => assertEquals(fixtureNtac(p), ntac)
      case _                              => fail("Expected 1 NGO.")
    }
  }

  @Test def testJointQueue() {
    val sub2  = im.NgoSubmission(
      request2,
      response2,
      im.NgoPartner.US,
      im.InvestigatorRef.empty
    )
    val p = new ProposalFixture {
      override def queueSubs: Either[List[im.NgoSubmission], im.ExchangeSubmission] =
        Left(List(ngoSubmission, sub2))
    }
    ntacIo.read(p.proposal) match {
      case Success(NonEmptyList(ntac1, ntac2)) =>
        assertEquals(fixtureNtac(p), ntac1)
        val expected = IList(Ntac(US, "zzz-123", Ntac.Rank(some(10.0)), Time.hours(2.0), poorWeather = false))
        assertEquals(expected, ntac2)
      case _                                   => fail("Expected 2 NGOs.")
    }
  }

  @Test def testExchangeQueue() {
    val sub = im.ExchangeSubmission(
      request2,
      response2,
      im.ExchangePartner.SUBARU,
      im.InvestigatorRef.empty
    )
    val p = new ProposalFixture {
      override def queueSubs: Either[List[im.NgoSubmission], im.ExchangeSubmission] = Right(sub)
    }
    // val SUBARU = Partner("SUBARU", "Subaru", 1.0, Set(Site.GN), "e@mail")
    val io = new NtacIo //(TestPartners.AllMap + ("SUBARU" -> SUBARU))
    io.read(p.proposal) match {
      case Success(NonEmptyList(ntac, _)) =>
        val expected = Ntac(SUBARU, "zzz-123", Ntac.Rank(some(10.0)), Time.hours(2.0), poorWeather = false)
        assertEquals(expected, ntac)
      case _                              => fail("Expected Subaru exchange")
    }
  }

  @Test def testLargeProgram() {
    val lpSub   = im.LargeProgramSubmission(request2, response2)
    val lpClass = im.LargeProgramClass(
      None,
      None,
      some(UUID.randomUUID()),
      lpSub,
      im.ToOChoice.None,
      None,
      false
    )

    val p = new ProposalFixture {
      override def proposalClass = lpClass
    }
    // val LP = Partner("LP", "Large Program", 1.0, Set(Site.GN), "e@mail")
    val io = new NtacIo //(TestPartners.AllMap + ("LP" -> LP))
    io.read(p.proposal) match {
      case Success(NonEmptyList(ntac, _)) =>
        val expected = Ntac(LP, "zzz-123", Ntac.Rank(some(10.0)), Time.hours(2.0), poorWeather = false, Some(p.pi.lastName))
        assertEquals(expected, ntac)
      case _                              =>
        fail("Expected LP")
    }
  }

  @Test def testUnexpectedProposalClass() {
    val p = new ProposalFixture {
      override def proposalClass = im.ExchangeProposalClass(
        None,
        None,
        some(UUID.randomUUID()),
        im.ExchangePartner.SUBARU,
        List(ngoSubmission)
      )
    }
    ntacIo.read(p.proposal) match {
      case Failure(NonEmptyList(msg, _)) => assertEquals(NtacIo.UNEXPECTED_PROPOSAL_CLASS, msg)
      case _                             => fail("Expecting a failure")
    }
  }

  @Test def testNoneAccepted() {
    val p = new ProposalFixture {
      override def submissionDecision = some(im.SubmissionDecision(Left(im.SubmissionReject)))
    }
    ntacIo.read(p.proposal) match {
      case Failure(NonEmptyList(msg, _)) => assertEquals(NtacIo.NONE_ACCEPTED, msg)
      case _                             => fail("Expecting a failure")
    }
  }

  @Test def testOneNoResponse() {
    def rejected(p: ProposalFixture) = im.NgoSubmission(
      p.submissionRequest,
      None,
      im.NgoPartner.BR,
      p.partnerLead
    )

    val p = new ProposalFixture {
      override def queueSubs: Either[List[im.NgoSubmission], im.ExchangeSubmission] =
        Left(List(ngoSubmission, rejected(this)))
    }

    ntacIo.read(p.proposal) match {
      case Success(NonEmptyList(ntac, _)) => assertEquals(fixtureNtac(p), ntac)
      case _                              => fail("Expected 1 NGO.")
    }
  }

  // @Test def testUnknownPartner() {
  //   val p  = new ProposalFixture
  //   val io = new NtacIo(TestPartners.AllMap - p.ngoPartner.name)
  //   io.read(p.proposal) match {
  //     case Failure(NonEmptyList(msg, _)) => assertEquals(NtacIo.UNKNOWN_PARTNER_ID(p.ngoPartner.name), msg)
  //     case _                             => fail("Expecting a failure")
  //   }
  // }
}