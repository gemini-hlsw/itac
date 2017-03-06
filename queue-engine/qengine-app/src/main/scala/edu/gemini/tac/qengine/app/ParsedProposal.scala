package edu.gemini.tac.qengine.app

import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal

/**
* Groups a proposal with its key and original P1 document.
*/
case class ParsedProposal(prop: Proposal, key: String, p1Doc: PhaseIProposal)