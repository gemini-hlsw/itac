package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.{check => api}
import edu.gemini.tac.service.check.impl.core.{ProposalIssue, StatelessProposalCheckFunction}

import edu.gemini.tac.persistence.RichProposal._
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}

import collection.JavaConversions._

object InstrumentCheck extends StatelessProposalCheckFunction {
  val name        = "Instruments"
  val description =
    "Proposal should contain instrument resources and they should all be " +
    "associated with just one site."

  def badSiteMessage(site: String) =
    "Proposal contains a resource with unrecognized site information '%s'".format(site)
  def noSiteMessage = "Proposal contains a resource with no site information."
  def badSite(p: Proposal, site: Site): ProposalIssue =
    site.name map {
      n => ProposalIssue.error(this, p, badSiteMessage(n), ProposalIssueCategory.Structural)
    } getOrElse {
      ProposalIssue.error(this, p, noSiteMessage, ProposalIssueCategory.Structural)
    }

  def mixedSiteMessage = "Proposal contains instruments for both sites."
  def mixedSite(p: Proposal): ProposalIssue = ProposalIssue.error(this, p, mixedSiteMessage, ProposalIssueCategory.Structural)

  // TODO: get rid of site, this should not be necessary
  // TODO: get rid of "no site" and "bad site" errors; this can not happen anymore
  case class Site(name: Option[String])
  object Site {
    val GN = Site(Some("North"))
    val GS = Site(Some("South"))

    def apply(name: String): Site =
      Site(Option(name))
//      Option(name) map {
//        _.toLowerCase match {
//          case "keck" | "subaru" => GN
//          case other => Site(Some(other))
//        }
//      } getOrElse Site(None)
  }

  private def sites(p: Proposal): Set[Site] =
    p.getPhaseIProposal.getBlueprints.map { bp => Site(bp.getInstrument.getSite.getDisplayName) }.toSet

  private def partition(s: Set[Site]): (Set[Site], Set[Site]) =
    s.partition { site => site == Site.GN || site == Site.GS }

  private def goodSiteIssues(p: Proposal, goodSites: Set[Site]): Set[api.ProposalIssue] =
    if (goodSites.size <= 1) Set.empty else Set(mixedSite(p))

  def apply(p: Proposal): Set[api.ProposalIssue] = {
    val allSites = sites(p)
    val (goodSites, badSites) = partition(allSites)
    (goodSiteIssues(p, goodSites)/:badSites) {
      (issueSet,site) => issueSet + badSite(p, site)
    }
  }

}