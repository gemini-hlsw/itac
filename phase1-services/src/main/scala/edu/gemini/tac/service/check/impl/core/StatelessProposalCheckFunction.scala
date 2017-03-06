package edu.gemini.tac.service.check.impl.core

import edu.gemini.tac.persistence.Proposal

import edu.gemini.tac.service.{check => api}
//import edu.gemini.tac.service.check.impl.core.ProposalIssue.noIssues
//import edu.gemini.tac.persistence.joints.JointProposal


/**
 * A proposal check function that examines only a single proposal and needs no
 * context or state.
 */
trait StatelessProposalCheckFunction extends (Proposal => Set[api.ProposalIssue]) with BaseProposalCheckFunction {
  /*
  def ifJoint(p: Proposal)(f: Proposal => Set[api.ProposalIssue]): Set[api.ProposalIssue] =
    p match {
      case jp: JointProposal => f(p)
      case _ => noIssues
    }

  def ifNotJoint(p: Proposal)(f: Proposal => Set[api.ProposalIssue]): Set[api.ProposalIssue] =
    p match {
      case jp: JointProposal => noIssues
      case _ => f(p)
    }
    */
}

