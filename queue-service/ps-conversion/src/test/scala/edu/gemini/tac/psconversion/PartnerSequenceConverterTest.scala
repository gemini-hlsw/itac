package edu.gemini.tac.psconversion

import edu.gemini.tac.qengine.api.config.PartnerSequence
import edu.gemini.tac.qengine.ctx.{TestPartners, Site}
import edu.gemini.tac.{persistence => ps}

import org.junit.Assert.{fail => failmsg, assertEquals}
import org.junit.Test

import scalaz._

class PartnerSequenceConverterTest {

  val psPartners = TestPartners.All.map { partner =>
    val psPartner = new ps.Partner()
    psPartner.setAbbreviation(partner.id)
    psPartner.setName(partner.fullName)
    psPartner.setPartnerCountryKey(partner.id)
    psPartner.setPercentageShare(partner.share.doubleValue)
    psPartner.setSiteShare(partner.sites.toList match {
      case List(Site.north) => ps.Partner.SiteShare.NORTH
      case List(Site.south) => ps.Partner.SiteShare.SOUTH
      case _                => ps.Partner.SiteShare.BOTH
    })
    psPartner
  }

  def psQueue(psSite: ps.Site, pick: ps.Partner): ps.queues.Queue = {
    val psQueue = new ps.queues.Queue()

    psQueue.setSite(psSite)
    psQueue.setPartnerWithInitialPick(pick)

    val partnerPercents = new java.util.HashSet[ps.phase1.queues.PartnerPercentage]()
    psPartners.foreach { psPartner =>
      val pp = new ps.phase1.queues.PartnerPercentage(psQueue, psPartner, psPartner.getPercentageShare / 100.0)
      partnerPercents.add(pp)
    }

    psQueue.setPartnerPercentages(partnerPercents)
    psQueue
  }

  val stubCommittee: ps.Committee = null

  private def fail(error: PsError): Unit = {
    error match {
      case BadData(msg) =>
        failmsg(s"BadData($msg)")
      case DatabaseError(ex) =>
        ex.printStackTrace()
        failmsg(ex.getMessage)
    }
  }

  private def expect(seq: PsError \/ PartnerSequence, ids: String*): Unit = {
    seq match {
      case \/-(s) => assertEquals(ids.toList, s.sequence.take(ids.length).toList.map(_.id))
      case -\/(e) => fail(e)
    }
  }

  @Test
  def createProportionalPartnerSequence(): Unit = {
    val seq = PartnerSequenceConverter.readProportional(psQueue(ps.Site.NORTH, null))
    expect(seq, "US", "CA")
  }

  @Test
  def createProportionalWithFirstPick(): Unit = {
    val q = psQueue(ps.Site.NORTH, psPartners.find(_.getAbbreviation == "CA").orNull)
    val seq = PartnerSequenceConverter.readProportional(q)
    expect(seq, "CA", "UH")
  }

  @Test
  def createCustomRepeating(): Unit = {
    val q     = psQueue(ps.Site.SOUTH, null)
    val psSeq = new ps.phase1.queues.PartnerSequence(stubCommittee, "My Seq", "AR, CL, US, GS, AU, BR, CA", true)
    val seq   = PartnerSequenceConverter.readCustom(psSeq, q)
    expect(seq, "AR", "CL", "US", "GS", "AU", "BR", "CA", "AR", "CL", "US", "GS", "AU", "BR", "CA", "AR", "CL")
  }

  @Test
  def createCustomRepeatingWithMissingPartners(): Unit = {
    val q     = psQueue(ps.Site.SOUTH, null)
    val psSeq = new ps.phase1.queues.PartnerSequence(stubCommittee, "My Seq", "AR, CL, US, GS, AU", true)  // missing BR, CA
    PartnerSequenceConverter.readCustom(psSeq, q) match {
      case -\/(BadData(msg)) => assertEquals(PartnerSequenceConverter.PARTNERS_MISSING_FROM_SEQUENCE(List(TestPartners.BR, TestPartners.CA)), msg)
      case _ => failmsg("Should be missing BR and CA")
    }
  }

  @Test
  def createCustomThenProportional(): Unit = {
    val q     = psQueue(ps.Site.SOUTH, null)
    val psSeq = new ps.phase1.queues.PartnerSequence(stubCommittee, "My Seq", "AR, CL, US, GS, AU", false)
    val seq   = PartnerSequenceConverter.readCustom(psSeq, q)
    // SW: this seems odd, the initial custom sequence is not accounted for
    // when computing the proportional one that follows.
    expect(seq, "AR", "CL", "US", "GS", "AU", "US", "CA", "CL")
  }

  @Test
  def createCustomWithFirstPick(): Unit = {
    val q     = psQueue(ps.Site.SOUTH, psPartners.find(_.getAbbreviation == "CL").orNull)
    val psSeq = new ps.phase1.queues.PartnerSequence(stubCommittee, "My Seq", "AR, CL, US, GS, AU, BR, CA", true)
    val seq   = PartnerSequenceConverter.readCustom(psSeq, q)
    expect(seq, "CL", "US", "GS", "AU", "BR", "CA", "AR", "CL")
  }
}