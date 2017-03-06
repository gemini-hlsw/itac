package edu.gemini.tac.persistence.phase1.submission;

import edu.gemini.model.p1.mutable.SubmissionResponse;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.Investigator;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;

@Entity
@DiscriminatorValue("LargeProgramSubmission")
public class LargeProgramSubmission extends Submission {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    protected LargeProgramSubmission() {}

    public LargeProgramSubmission(edu.gemini.model.p1.mutable.LargeProgramSubmission that, Partner partner) {
        setRequest(new SubmissionRequest(that.getRequest()));

        final SubmissionResponse response = that.getResponse();
        if (response != null) {
            if (response.getAccept() != null)
                setAccept(new SubmissionAccept(response.getAccept()));
            if (response.getReceipt() != null)
                setReceipt(new SubmissionReceipt(response.getReceipt()));
            setRejected(response.getReject() != null);
            setComment(StringUtils.trim(response.getComment()));
        } else {
            setRejected(Boolean.FALSE);
        }

        this.partner = partner;
    }

    public LargeProgramSubmission(Partner partner, SubmissionRequest request, SubmissionReceipt receipt, SubmissionAccept accept, boolean isRejected) {
        super(request, receipt, accept, isRejected);

        Validate.isTrue(partner.isLargeProgram());

        this.partner = partner;
    }

    @Override
    public String toString() {
        return "LargeProgramSubmission";
    }

    @Override
    public Partner getPartner() {
        return partner;
    }

    public edu.gemini.model.p1.mutable.LargeProgramSubmission toMutable() {
        final edu.gemini.model.p1.mutable.LargeProgramSubmission mSubmission = new edu.gemini.model.p1.mutable.LargeProgramSubmission();
        super.toMutable(mSubmission);

        return mSubmission;
    }

}
