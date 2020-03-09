package edu.gemini.tac.qengine.log

import edu.gemini.tac.qengine.p1.Proposal

/**
 * A trait that marks all proposal rejection messages.
 */
trait RejectMessage extends LogMessage with ProposalDetailMessage