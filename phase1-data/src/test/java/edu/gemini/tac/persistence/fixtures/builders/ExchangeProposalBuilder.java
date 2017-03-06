package edu.gemini.tac.persistence.fixtures.builders;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.proposal.ExchangeProposal;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import org.apache.commons.lang.Validate;

public class ExchangeProposalBuilder extends ProposalBuilder {

    private ExchangeProposal exchangeProposal;
    
    public ExchangeProposalBuilder() {
        super(new ExchangeProposal());
        this.exchangeProposal = (ExchangeProposal) phaseIProposal;
    }

    protected void build() {
        // do final touch ups of base class
        super.build();

        // set exchange partner (KECK or SUBARU)
        Validate.isTrue(exchangeProposal.getBlueprints().size() > 0);
        Validate.isTrue(exchangeProposal.getBlueprints().get(0).getInstrument().isExchange());
        exchangeProposal.setPartner(exchangeProposal.getBlueprints().get(0).getInstrument().getExchangePartner());

        // assuming there have been no ngo parts added already
        // (this is a pre-condition for the current state of the code only, if you change
        // the builder logic, this pre-condition might become invalid, change as needed)
        Validate.isTrue(exchangeProposal.getSubmissions().size() == 0);

        Validate.isTrue(exchangeProposal.getNgos().size() == 0);

        // add all requests (if there are any)
        for (Partner partner : requests.keySet()) {
            NgoSubmission submission = new NgoSubmission();
            submission.setPartner(partner);
            submission.setAccept(null);
            submission.setReceipt(null);
            submission.setRequest(requests.get(partner));
            submission.setPartnerLead(exchangeProposal.getInvestigators().getPi());
            submission.setRejected(false);
            submission.setComment("");
            exchangeProposal.getNgos().add(submission);
        }

        // this is an exchange from an ngo partner FOR an exchange partner
        NgoSubmission submission = new NgoSubmission();
        submission.setPartner(proposal.getPartner());
        submission.setAccept(accept);
        submission.setReceipt(receipt);
        submission.setRequest(request);
        submission.setPartnerLead(exchangeProposal.getInvestigators().getPi());
        submission.setRejected(false);
        submission.setComment("");
        exchangeProposal.getNgos().add(submission);
    }
}
