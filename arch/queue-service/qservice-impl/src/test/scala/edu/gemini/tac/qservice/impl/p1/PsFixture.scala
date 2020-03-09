package edu.gemini.tac.qservice.impl.p1

import edu.gemini.tac.persistence.{Partner => PsPartner, Proposal => PsProposal}
import edu.gemini.tac.persistence.fixtures.builders.{JointProposalBuilder, ProposalBuilder, QueueProposalBuilder}
import edu.gemini.tac.persistence.phase1.{TimeAmount, TimeUnit}
import edu.gemini.tac.persistence.phase1.blueprint.{BlueprintBase => PsBlueprintBase}
import edu.gemini.tac.persistence.phase1.blueprint.niri.{NiriBlueprint => PsNiriBlueprint}

import edu.gemini.tac.qengine.ctx.{Context, Site, Semester, Partner, TestPartners}
import edu.gemini.tac.qengine.p1.{Ntac, CoreProposal, JointProposal}

import java.util.Date
import edu.gemini.tac.qengine.util.Time

/**
 * A few pre-created proposals to facilitate testing.
 */
object PsFixture {
  import TestPartners._
  val partners = All

  val psPartners = partners.map { createPsPartner }

  def createPsPartner(p: Partner): PsPartner = {
    val ps = new PsPartner()
    ps.setName(p.fullName)
    ps.setAbbreviation(p.id)
    ps.setIsCountry(true)
    ps.setPartnerCountryKey(p.id)
    ps.setNgoFeedbackEmail(p.id.toLowerCase + "@gemini.edu")
    ps.setSiteShare(p.sites.toList match {
      case List(Site.north) => PsPartner.SiteShare.NORTH
      case List(Site.south) => PsPartner.SiteShare.SOUTH
      case _                => PsPartner.SiteShare.BOTH
    })
    ps.setPercentageShare(p.share.doubleValue / 100.0)
    ps
  }


  val site = Site.north
  val semester = new Semester(2014, Semester.Half.A)
  val ctx = new Context(site, semester)

  val committee = ProposalBuilder.dummyCommittee
  val bps = new java.util.ArrayList[PsBlueprintBase]()
  val bp = new PsNiriBlueprint()
  bps.add(bp)

  val (coreAR1, psAR1) = mkCorePair(ProposalBuilder.dummyPartnerAR, AR, "ar1")
  val (coreAR2, psAR2) = mkCorePair(ProposalBuilder.dummyPartnerAR, AR, "ar2")
  val (coreBR1, psBR1) = mkCorePair(ProposalBuilder.dummyPartnerBR, BR, "br1")

  val jointAR1_BR1: JointProposal = JointProposal("j1", coreAR1, List(coreAR1.ntac, coreBR1.ntac))
  val psJointAR1_BR1 = {
    val jointAr1 = mkProposal(ProposalBuilder.dummyPartnerAR, "ar1")
    val jointBr1 = mkProposal(ProposalBuilder.dummyPartnerBR, "br1")
    new JointProposalBuilder().
      addProposal(jointAr1).
      addProposal(jointBr1).
      create()
  }

  def mkCorePair(psPartner: PsPartner, partner: Partner, ref: String): (CoreProposal, PsProposal) = {
    val ps = mkProposal(psPartner, ref)
    val cr = CoreProposal(Ntac(partner, ref, 1.0, Time.hours(10.0)), site)
    (cr, ps)
  }

  def mkProposal(partner: PsPartner, ref: String): PsProposal =
    new QueueProposalBuilder().
      setCommittee(committee).
      setPartner(partner).
      setAccept(new TimeAmount(10, TimeUnit.HR), new TimeAmount(10, TimeUnit.HR), 1).
      setReceipt(ref, new Date()).
      create()
}
