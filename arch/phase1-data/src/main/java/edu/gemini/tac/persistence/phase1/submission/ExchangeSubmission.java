package edu.gemini.tac.persistence.phase1.submission;

import edu.gemini.model.p1.mutable.ExchangePartner;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.Investigator;
import org.apache.commons.lang.Validate;

import javax.persistence.*;
import java.util.Map;

/**
 *       An exchange submission ties an exchange partner with request and tac
 *       information.
 */
@Entity
@DiscriminatorValue("ExchangeSubmission")
public class ExchangeSubmission extends PartnerSubmission {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    public ExchangeSubmission() {}

    public ExchangeSubmission(final ExchangeSubmission src) {
        setPartnerLead(src.getPartnerLead());
        setPartner(src.getPartner());
        setRejected(src.getRejected());
        setRequest(new SubmissionRequest(src.getRequest()));
        setReceipt(new SubmissionReceipt(src.getReceipt()));
        setAccept(new SubmissionAccept(src.getAccept()));
    }

    public ExchangeSubmission(Partner exchangePartner, SubmissionRequest submissionRequest, SubmissionReceipt submissionReceipt, SubmissionAccept submissionAccept, boolean isRejected) {
        super(submissionRequest, submissionReceipt, submissionAccept, isRejected);

        Validate.isTrue(exchangePartner.isExchange());

        this.partner = exchangePartner;
    }


    public ExchangeSubmission(edu.gemini.model.p1.mutable.ExchangeSubmission mExchange, final Map<String, Investigator> investigatorMap, final Map<String, Partner> partnerCountryKeyToPartner) {
        super(mExchange, investigatorMap);
        setPartner(partnerCountryKeyToPartner.get(mExchange.getPartner().name()));
    }

    @Override
    public String toString() {
        return "ExchangeSubmission{" +
                "partner=" + partner +
                "} " + super.toString();
    }

    public void setPartner(Partner partner) {
        Validate.isTrue(partner.isExchange());
        this.partner = partner;
    }

    public Partner getPartner() {
        return partner;
    }

    public edu.gemini.model.p1.mutable.ExchangeSubmission toMutable(final Investigator partnerLead, final String partnerLeadInvestigatorId) {
        final edu.gemini.model.p1.mutable.ExchangeSubmission mSubmission = new edu.gemini.model.p1.mutable.ExchangeSubmission();
        super.toMutable(mSubmission, partnerLead, partnerLeadInvestigatorId);
        mSubmission.setPartner(exchangePartnerForPartner(partner));
        return mSubmission;
    }
    
    private ExchangePartner exchangePartnerForPartner(Partner partner) {
        if (partner.getPartnerCountryKey().equals(ExchangePartner.KECK.name())) {
            return ExchangePartner.KECK;
        } else if (partner.getPartnerCountryKey().equals(ExchangePartner.SUBARU.name())) {
            return ExchangePartner.SUBARU;
        } else if (partner.getPartnerCountryKey().equals(ExchangePartner.CFH.name())) {
            return ExchangePartner.CFH;
        } else {
            throw new IllegalArgumentException("not a valid exchange partner " + partner.getName());
        }
    }

    @Override
    protected String myDot(){
        return "ExchangeSubmission_" + (id == null ? "NULL" : id.toString());
    }

    public boolean isExchange() { return true; }
}
