package edu.gemini.tac.persistence.fixtures.builders;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.Itac;

/**
 * A very rudimentary joint proposal builder for testing.
 */
public class JointProposalBuilder {
    
    private JointProposal joint;

    public JointProposalBuilder() {
        joint = new JointProposal();
    }
    
    public JointProposalBuilder setPrimaryProposal(Proposal p) {
        joint.setPrimaryProposal(p);
        return this;
    }

    public JointProposalBuilder addProposal(Proposal p) {
        joint.add(p);
        // this relation is usually established by Hibernate on saving, but in case we're using
        // the proposal for "in-memory" only testing, we want this relation to be established
        p.setJointProposal(joint);
        return this;
    }

    public JointProposal create() {
        // copy the itac information from the primary proposal into the joint proposal itac
        // (some tests need useful defaults here)
        Itac jointItac = joint.getItac();
        Itac primaryItac = joint.getPrimaryProposal().getItac();
        jointItac.setAccept(primaryItac.getAccept());
        jointItac.setRejected(primaryItac.getRejected());
        jointItac.setComment(primaryItac.getComment());

        // done
        return joint;
    }

}
