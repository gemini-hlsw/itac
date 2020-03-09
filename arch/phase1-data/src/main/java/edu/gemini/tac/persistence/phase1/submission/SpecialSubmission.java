package edu.gemini.tac.persistence.phase1.submission;

import edu.gemini.model.p1.mutable.SpecialProposalType;
import edu.gemini.tac.persistence.Partner;
import org.apache.commons.lang.NotImplementedException;

import javax.persistence.*;

@Entity
@DiscriminatorValue("SpecialSubmission")
public class SpecialSubmission  extends Submission {
    @Enumerated(EnumType.STRING)
    @Column(name = "special_proposal_type", nullable = true)
    protected SpecialProposalType specialProposalType;

    public SpecialSubmission() {}
    public SpecialSubmission(final SpecialSubmission src) {
        setSpecialProposalType(src.getSpecialProposalType());
    }

    public SpecialSubmission(final edu.gemini.model.p1.mutable.SpecialSubmission mSubmission) {
        setSpecialProposalType(mSubmission.getType());
    }

    @Override
    public String toString() {
        return "SpecialSubmission{" +
                "specialProposalType=" + specialProposalType +
                "} " + super.toString();
    }

    @Override
    public Partner getPartner() {
        throw new NotImplementedException("Special submissions are not associated with partners.");
    }

    public edu.gemini.model.p1.mutable.SpecialSubmission toMutable() {
        final edu.gemini.model.p1.mutable.SpecialSubmission mSubmission = new edu.gemini.model.p1.mutable.SpecialSubmission();
        super.toMutable(mSubmission);

        mSubmission.setType(getSpecialProposalType());

        return mSubmission;
    }

    public SpecialProposalType getSpecialProposalType() {
        return specialProposalType;
    }

    public void setSpecialProposalType(SpecialProposalType specialProposalType) {
        this.specialProposalType = specialProposalType;
    }
}
