package edu.gemini.tac.service.check;

import edu.gemini.tac.persistence.Proposal;

import java.util.Collection;
import java.util.Set;

/**
 * Defines the contract implemented by a proposal checker.
 */
public interface ProposalChecker {
    /**
     * Gets the collection of proposal checks that will be performed.
     */
    Set<ProposalCheck> checks();

    /**
     * Executes all the checks on all the given proposals.
     *
     * @param proposals proposals to examine
     *
     * @return Set of proposal issues that were found, if any
     */
    Set<ProposalIssue> exec(Collection<Proposal> proposals);
}
