package edu.gemini.tac.persistence.phase1.submission;

import edu.gemini.model.p1.mutable.*;
import edu.gemini.model.p1.mutable.PartnerSubmission;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.Investigator;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.util.Map;

/**
 * Base class for NgoSubmission / ExchangeSubmission
 */

@NamedQueries({
        @NamedQuery(name = "Submission.findNgoSubmissions",
                query = "from NgoSubmission s " +
                        "left join fetch s.partner p " +
                        "left join fetch s.request r " +
                        "where s in (:submissions)"
        ),
        @NamedQuery(name = "Submission.findExchangeSubmissions",
                query = "from ExchangeSubmission s " +
                        "left join fetch s.partner p " +
                        "left join fetch s.request r " +
                        "where s in (:submissions)"
        )
})
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Table(name = "v2_submissions")
public abstract class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Column(name = "rejected")
    protected Boolean rejected = Boolean.FALSE;

    @Embedded
    protected SubmissionAccept accept;

    @Embedded
    protected SubmissionReceipt receipt;

    @Embedded
    protected SubmissionRequest request;

    @Column(name = "comment")
    protected String comment;

    public Submission() {
    }

    public Submission(Submission that) {
        if (that.getRequest() != null) setRequest(new SubmissionRequest(that.getRequest()));
        if (that.getReceipt() != null) setReceipt(new SubmissionReceipt(that.getReceipt()));
        if (that.getAccept() != null) setAccept(new SubmissionAccept(that.getAccept()));
        setRejected(that.getRejected());
        setComment(that.getComment());
    }

    public Submission(final PartnerSubmission mPartnerSubmission) {
        setRequest(new SubmissionRequest(mPartnerSubmission.getRequest()));

        final SubmissionResponse response = mPartnerSubmission.getResponse();
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
    }

    @Override
    public String toString() {
        return "Submission{" +
                "id=" + id +
                ", rejected=" + rejected +
                ", accept=" + accept +
                ", receipt=" + receipt +
                ", request=" + request +
                '}';
    }

    public Submission(SubmissionRequest request, SubmissionReceipt receipt, SubmissionAccept accept, boolean isRejected){
        this.request = request;
        this.receipt = receipt;
        this.accept = accept;
        this.rejected = isRejected;
        this.comment = "None.";
    }

    protected String myDot() { return "Submission"; }

    public String toDot() {
        String myDot = myDot();
        String dot = myDot + ";\n";

        return dot;
    }

    public abstract Partner getPartner();

    public SubmissionRequest getRequest() {
        return request;
    }

    public void setRequest(SubmissionRequest request) {
        this.request = request;
    }

    public Boolean getRejected() {
        return rejected;
    }

    public void setRejected(Boolean rejected) {
        this.rejected = rejected;
    }

    public SubmissionAccept getAccept() {
        return accept;
    }

    public void setAccept(SubmissionAccept accept) {
        this.accept = accept;
    }

    public SubmissionReceipt getReceipt() {
        return receipt;
    }

    public void setReceipt(SubmissionReceipt receipt) {
        this.receipt = receipt;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    protected edu.gemini.model.p1.mutable.Submission toMutable(edu.gemini.model.p1.mutable.Submission mSubmission) {

        mSubmission.setRequest(getRequest().toMutable());

        if (getAccept() != null || getReceipt() != null || getComment() != null) {
            // create response part only if there's actually data to be stored in it
            final edu.gemini.model.p1.mutable.SubmissionResponse mResponse = new edu.gemini.model.p1.mutable.SubmissionResponse();
            mSubmission.setResponse(mResponse);
            if (getAccept() != null)
                mResponse.setAccept(getAccept().toMutable());
            if (getReceipt() != null)
                mResponse.setReceipt(getReceipt().toMutable());
            if (rejected != null && rejected) // Only an affirmative decision to reject should be passed on.
                mResponse.setReject(new SubmissionReject());
            mResponse.setComment(getComment());
        }

        return mSubmission;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isExchange() { return false; }
}
