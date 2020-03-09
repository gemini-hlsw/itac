package edu.gemini.tac.persistence.fixtures.builders;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.proposal.ClassicalProposal;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt;
import org.apache.commons.lang.Validate;

import java.util.Date;

public class ClassicalProposalBuilder extends ProposalBuilder {

    private ClassicalProposal classicalProposal;

    public ClassicalProposalBuilder() {
        super(new ClassicalProposal());
        this.classicalProposal = (ClassicalProposal) phaseIProposal;
    }

    protected void build() {
        // do final touch ups of base class
        super.build();

        // build ngo submission stuff
        buildSubmissionElements(classicalProposal);

    }
}
