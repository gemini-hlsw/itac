package edu.gemini.tac.persistence.fixtures.builders;

import edu.gemini.model.p1.mutable.TooOption;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.proposal.QueueProposal;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt;
import org.apache.commons.lang.Validate;
import java.util.Date;

/**
 */
public class QueueProposalBuilder extends ProposalBuilder {
    
    private QueueProposal queueProposal;

    public QueueProposalBuilder() {
        this(TooOption.NONE);
    }

    public QueueProposalBuilder(TooOption tooOption) {
        super(new QueueProposal());
        this.queueProposal = (QueueProposal) phaseIProposal;
        this.queueProposal.setTooOption(tooOption);
    }

    @Override
    protected void build() {
        // do final touch ups of base class
        super.build();

        // build ngo submission stuff
        buildSubmissionElements(queueProposal);
    }

}
