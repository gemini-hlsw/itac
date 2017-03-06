package edu.gemini.tac.persistence

import edu.gemini.tac.persistence.RichProposal._

/**
 * A predicate that determines whether the given proposal is an exchange
 * proposal.  An exchange proposal can be either from an exchange partner
 * for time on Gemini, or else from a Gemini partner for time on another
 * telescope (Keck or Subaru).

 * Refactored during the great model change of 2011 to use the methods on
 * the class in order to reduce the sources of information about what makes
 * something an exchange proposal.
 */
object IsExchange extends (Proposal => Boolean) {
  def isFromExchangePartner(prop: Proposal): Boolean =
    prop.isFromExchangePartner

  def isForExchangePartner(prop: Proposal): Boolean =
    prop.isForExchangePartner

  def apply(prop: Proposal): Boolean =
    isForExchangePartner(prop) || isFromExchangePartner(prop)
}