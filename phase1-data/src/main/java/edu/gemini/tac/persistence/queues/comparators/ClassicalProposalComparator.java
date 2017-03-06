package edu.gemini.tac.persistence.queues.comparators;

import edu.gemini.tac.persistence.Proposal;

import java.util.Comparator;

/**
 * Simple PI last name comparator to support ITAC-461
 */
public class ClassicalProposalComparator implements Comparator <Proposal> {
    public ClassicalProposalComparator() {}

    public int compare(Proposal one, Proposal two) {
        final String nameOne = one.getPhaseIProposal().getInvestigators().getPi().getLastName().toLowerCase().trim();
        final String nameTwo = two.getPhaseIProposal().getInvestigators().getPi().getLastName().toLowerCase().trim();

        return nameOne.compareTo(nameTwo);
    }
}