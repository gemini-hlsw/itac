package edu.gemini.tac.persistence.phase1.submission;

import edu.gemini.tac.persistence.phase1.Investigator;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Map;

@Entity
public abstract class PartnerSubmission extends Submission {
    @ManyToOne(fetch = FetchType.LAZY, optional = true, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "partner_lead_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Investigator partnerLead;

    public PartnerSubmission() {}

    public PartnerSubmission(final PartnerSubmission copiedSubmission) {
        super(copiedSubmission);

        setPartnerLead(copiedSubmission.getPartnerLead());
    }

    public PartnerSubmission(SubmissionRequest request, SubmissionReceipt receipt, SubmissionAccept accept, boolean isRejected){
        super(request, receipt, accept, isRejected);
    }

    public PartnerSubmission(edu.gemini.model.p1.mutable.PartnerSubmission mNgoSubmission, final Map<String, Investigator> investigatorMap) {
        super(mNgoSubmission);

        setPartnerLead(investigatorMap.get(mNgoSubmission.getPartnerLead().getId()));
    }

    protected edu.gemini.model.p1.mutable.Submission toMutable(edu.gemini.model.p1.mutable.Submission mSubmission, final Investigator partnerLead, final String partnerLeadInvestigatorId) {
        super.toMutable(mSubmission);

        if (partnerLeadInvestigatorId != null) {
            edu.gemini.model.p1.mutable.Investigator i = partnerLead.toMutable(partnerLeadInvestigatorId);
            ((edu.gemini.model.p1.mutable.PartnerSubmission)mSubmission).setPartnerLead(i);
        }

        return mSubmission;
    }


    @Override
    protected String myDot(){
        return "PartnerSubmission_" + (id == null ? "NULL" : id.toString());
    }

    @Override
    public String toString() {
        return "PartnerSubmission{" +
                "partnerLead=" + partnerLead +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartnerSubmission)) return false;

        PartnerSubmission that = (PartnerSubmission) o;

        if (partnerLead != null ? !partnerLead.equals(that.partnerLead) : that.partnerLead != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return partnerLead != null ? partnerLead.hashCode() : 0;
    }

    public Investigator getPartnerLead() {
        return partnerLead;
    }

    public void setPartnerLead(Investigator partnerLead) {
        this.partnerLead = partnerLead;
    }
}
