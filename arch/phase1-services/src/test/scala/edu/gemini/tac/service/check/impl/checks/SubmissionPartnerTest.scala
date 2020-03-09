package edu.gemini.tac.service.check.impl.checks

import org.junit._
import Assert._


import edu.gemini.tac.service.check.impl.checks.{SubmissionPartnerCheck => Ck}
import edu.gemini.tac.persistence.{Partner, Proposal}
import edu.gemini.tac.persistence.phase1.{TimeAmount, TimeUnit}
import edu.gemini.tac.persistence.fixtures.builders.{JointProposalBuilder, QueueProposalBuilder, ProposalBuilder}
import edu.gemini.tac.persistence.fixtures.builders.ProposalBuilder.{dummyPartnerCL=>CL, dummyPartnerUS=>US, dummyPartnerAU=>AU}
import edu.gemini.tac.persistence.phase1.proposal.GeminiNormalProposal

import scala.collection.JavaConversions._
import edu.gemini.tac.persistence.phase1.submission.SubmissionAccept

class SubmissionPartnerTest {
  def brokenProposal(specs: (Partner, Boolean)*) = {
    val builder = new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(US)
    // set request part for main partner and add request parts for all other partners
    specs.foreach(pb => {
      if (pb._1 == US) {
        builder.setRequest(new TimeAmount(1, TimeUnit.HR), new TimeAmount(1, TimeUnit.HR))
      } else {
        builder.addRequest(pb._1, new TimeAmount(1, TimeUnit.HR), new TimeAmount(1, TimeUnit.HR))
      }
    })
    // create a valid proposal with one default accept for US (this is done by the builder by default)
    val p = builder.create()
    val p1p = p.getPhaseIProposal.asInstanceOf[GeminiNormalProposal]

    // and now: ouch!! we're building a broken proposal here, pfui!
    // the builder does not allow to build broken proposals, so we have to manually mess with it
    p1p.getSubmissions.foreach(s => {
      if (specs.contains((s.getPartner, true))) {
        // put together an accept part
        var accept = new SubmissionAccept();
        accept.setEmail("kk");
        accept.setMinRecommend(new TimeAmount(1, TimeUnit.HR))
        accept.setPoorWeather(false);
        accept.setRanking(new java.math.BigDecimal("1"))
        accept.setRecommend(new TimeAmount(1,TimeUnit.HR))
        s.setAccept(accept);
      } else {
        s.setAccept(null); // main partner has accept part set by builder, remove it if necessary
      }
    })
    p
  }

  private def saneProposal(acceptPartner: Partner, otherPartners: Seq[Partner] = Seq()) = {
    val builder = new QueueProposalBuilder().
      setCommittee(ProposalBuilder.dummyCommittee).
      setPartner(acceptPartner)
    // add request parts for all partners (except for main partner)
    otherPartners.foreach(p => {
      if (p != acceptPartner) {
        builder.addRequest(p, new TimeAmount(1, TimeUnit.HR), new TimeAmount(1, TimeUnit.HR))
      }
    })
    // create a valid proposal with default request and accept for main partner (this is done by the builder by default)
    builder.create()
  }

  private def jointProposal(partners: Partner*): Proposal = {
    val builder = new JointProposalBuilder()
    partners.foreach(p => builder.addProposal(saneProposal(p, partners)))
    builder.create()
  }

  @Test def passesSinglePartnerOneAccept() {
    val p = saneProposal(US)
    val issues = Ck(p)
    assertEquals(Set.empty, issues)
  }

  @Test def passesMultiplePartnersOneAccept() {
    val p = saneProposal(US, Seq(AU, CL))
    val issues = Ck(p)
    assertEquals(Set.empty, issues)
  }

  @Test def detectsNoAccepts() {
    val jp = brokenProposal((US, false), (AU, false), (CL, false))
    val issues = Ck(jp)
    assertEquals(Ck.noSubmissionFlags(jp), issues)
  }

  @Test def detectsDuplicateAccepts() {
    val p = brokenProposal((US, true), (AU, true), (CL, false))
    val issues = Ck(p)
    assertEquals(Ck.multipleSubmissionFlags(p, 2), issues)
  }

  @Test def detectsMultipleAccepts() {
    val p = brokenProposal((US, true), (AU, true), (CL, true))
    val issues = Ck(p)
    assertEquals(Ck.multipleSubmissionFlags(p, 3), issues)
  }

  @Test def jointWithMultipleAcceptsIsOk() {
    val jp = jointProposal(US, AU, CL)
    val issues = Ck(jp)
    assertEquals(Set.empty, issues)
  }
}