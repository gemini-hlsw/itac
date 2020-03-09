package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.queues.PartnerSequence;
import edu.gemini.tac.persistence.queues.Queue;

import java.util.List;

/**
 * Committee centric database interaction.  The home for the details about what parts of the
 * object graph get populated in response to what input data.  Contains (and will contain more)
 * many methods for just the right amount of data to serve some particular function.
 * 
 * @author ddawson
 *
 */
/**
 * @author ddawson
 *
 */
public interface ICommitteeService {
	List<Committee> getAllCommittees();
	List<Committee> getAllActiveCommittees();
	Committee getCommittee(final Long committeeId);
    Committee getCommitteeWithMemberships(final Long committeeId);
	/**
	 * In support of the exchange analysis functionality, which requires us to go fairly
	 * deep into the submissions tree, but not particularly deep anywhere else.
	 * @param committeeId
	 * 
	 * @return a committee with proposals and accompanying submission maps populated
	 */
	Committee getCommitteeForExchangeAnalysis(final Long committeeId);

    List<Proposal> getProposalForCommittee(final Long committeeId, final Long proposalId);
    List<Proposal> checkProposalForCommittee(final Long committeeId, final Long proposalId);
	List<Proposal> getAllProposalsForCommittee(final Long committeeId);
    List<Proposal> getAllComponentsForCommittee(final Long committeeId);
    List<Proposal> checkAllProposalsForCommittee(final Long committeeId);
    List<Proposal> checkAllComponentsForCommittee(final Long committeeId);
    List<Proposal> getAllProposalsForCommitteePartner(final Long committeeId, final String partnerAbbreviation);

	List<Queue> getAllQueuesForCommittee(final Long committeeId);

    List<Queue> getAllQueuesForCommittee(final Long committeeId, final Long siteId);

	/**
	 * Save or update the committee in the database.
	 * 
	 * @param committee
	 */
	void saveCommitee(final Committee committee);
	/**
	 * Populates graph to support queue creation
	 * @param committeeId
	 * @return
	 */
	List<Proposal> getAllCommitteeProposalsForQueueCreation(Long committeeId);

	/**
	 * Via sideeffects, populates the session with various information related to the proposal
	 * list that is necessary for the various listings of proposals on display.
	 *  
	 * @param proposals
	 */
    void populateProposalsWithListingInformation(final List<Proposal> proposals);

    void setPartnerSequenceProportional(Long committeeId);
    void setPartnerSequence(Long committeeId, PartnerSequence ps);
    PartnerSequence getPartnerSequenceOrNull(Long committeeId);

    /**
     * Deletes all committee-based links pointing "inbound" to the JointProposal (i.e., queue-related links)
     * @param committee
     * @param jp
     */
    void dissolve(Committee committee, JointProposal jp);
}
