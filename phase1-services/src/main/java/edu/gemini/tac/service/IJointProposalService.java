package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.joints.JointProposal;

/**
 * Manipulates JointProposals
 *
 * Author: lobrien
 * Date: 1/25/11
 */
public interface IJointProposalService extends IProposalService{
    JointProposal mergeProposals(Proposal master, Proposal secondary);
    JointProposal add(JointProposal master, Proposal secondary);
    Proposal recreateWithoutComponent(Long committeeId, Long masterId, Long componentId);

    /**
     * Deletes the JointProposal *AND* the component proposals. Note that for the "dissolve"
     * use-case, use CommitteeService.dissolve() (since dissolving goes beyond Proposals and into Bandings)
     * @param master
     */
    void delete(JointProposal master);


    /**
     * Modifies a JointProposal so that it is controlled by the 'master' proposal.
     *
     * @param merged
     * @param newMaster
     * @return
     */
    JointProposal duplicateWithNewMaster(JointProposal merged, Proposal newMaster);
}
