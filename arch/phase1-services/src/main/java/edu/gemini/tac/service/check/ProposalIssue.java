package edu.gemini.tac.service.check;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.ProposalIssueCategory;

/**
 * Defines an issue found by the {@link ProposalChecker}.
 */
public interface ProposalIssue {
    enum Severity {
        warning,
        error,
        ;
    }

    /**
     * Gets the issue severity.
     */
    Severity severity();

    /**
     * Gets the propposal check information for the issue, identifiying which
     * check failed.
     */
    ProposalCheck check();

    /**
     * Gets the proposal in which the issue was found.
     */
    Proposal proposal();

    /**
     * Gets a more detailed message about the issue that was found.
     */
    String message();


    /**
     * Gets the category in which this issue resides
     */
    ProposalIssueCategory category();
}
