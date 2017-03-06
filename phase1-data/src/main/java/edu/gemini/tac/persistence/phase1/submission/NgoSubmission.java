package edu.gemini.tac.persistence.phase1.submission;

import edu.gemini.model.p1.mutable.NgoPartner;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.Investigator;
import org.apache.commons.lang.Validate;

import javax.persistence.*;
import java.util.Map;

/**
 * An NGO submission ties an NGO partner with request and tac information.
 */
@Entity
@DiscriminatorValue("NgoSubmission")
public class NgoSubmission extends PartnerSubmission {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    public NgoSubmission() {}

    public NgoSubmission(final NgoSubmission copiedSubmission) {
        super(copiedSubmission);
        setPartner(copiedSubmission.getPartner());
    }

    public NgoSubmission(Partner partner, SubmissionRequest request, SubmissionReceipt receipt, SubmissionAccept accept, boolean isRejected) {
        super(request, receipt, accept, isRejected);

        Validate.isTrue(partner.isNgo());

        this.partner = partner;
    }

    public NgoSubmission(final edu.gemini.model.p1.mutable.NgoSubmission mNgoSubmission,
                         final Map<String, Investigator> investigatorMap,
                         final Map<String, Partner> countryKeyToPartnerMap) {
        super(mNgoSubmission, investigatorMap);
        setPartner(countryKeyToPartnerMap.get(mNgoSubmission.getPartner().name()));
    }

    @Override
    public String toString() {
        return "NgoSubmission{" +
                "partner=" + partner +
                "} " + super.toString();
    }

    public void setPartner(final Partner partner) {
        final String partnerCountryKey = partner.getPartnerCountryKey();
        Validate.isTrue(partner.isNgo() || partnerCountryKey.equals("GS"));
        this.partner = partner;
    }

    @Override
    public Partner getPartner() {
         return partner;
     }

    @Override
    protected String myDot(){
        return "NgoSubmission_" + (id == null ? "NULL" : id.toString());
    }

    public edu.gemini.model.p1.mutable.NgoSubmission toMutable(final Investigator partnerLead, final String partnerLeadInvestigatorId) {
        final edu.gemini.model.p1.mutable.NgoSubmission mSubmission = new edu.gemini.model.p1.mutable.NgoSubmission();
        super.toMutable(mSubmission, partnerLead, partnerLeadInvestigatorId);
        for (NgoPartner p : NgoPartner.values()) {
            if (p.name().equals(partner.getPartnerCountryKey()))
                mSubmission.setPartner(p);
        }

        return mSubmission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NgoSubmission)) return false;
        if (!super.equals(o)) return false;

        NgoSubmission that = (NgoSubmission) o;

        if (partner != null && partner.getAbbreviation() != null ? !partner.equals(that.partner) : that.partner != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (partner != null && partner.getAbbreviation() != null ? partner.hashCode() : 0);
        return result;
    }
}
