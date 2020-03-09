package edu.gemini.tac.persistence.phase1.proposal;

import edu.gemini.model.p1.mutable.Proposal;
import edu.gemini.model.p1.mutable.QueueProposalClass;
import edu.gemini.model.p1.mutable.TooOption;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.Investigator;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionRequest;
import org.apache.commons.lang.Validate;
import org.hibernate.LazyInitializationException;

import javax.persistence.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Entity
@DiscriminatorValue("QueueProposal")
public class QueueProposal extends GeminiNormalProposal {
    @Embedded
    @AttributeOverrides( {
        @AttributeOverride(name="time.value", column = @Column(name="band3_time_value") ),
        @AttributeOverride(name="time.units", column = @Column(name="band3_time_units") ),
        @AttributeOverride(name="minTime.value", column = @Column(name="band3_min_time_value") ),
        @AttributeOverride(name="minTime.units", column = @Column(name="band3_min_time_units") )
    } )
    protected SubmissionRequest band3Request;

    @Column(name = "too_option")
    @Enumerated(EnumType.STRING)
    protected TooOption tooOption;

    public QueueProposal() {}

    public QueueProposal(final QueueProposal src) { this(src, CloneMode.FULL); }
    public QueueProposal(final QueueProposal src, final PhaseIProposal.CloneMode cloneMode) {
        super(src, cloneMode);
        try {
            if (src.getBand3Request() != null) // Don't create a band 3 request in the copy if one doesn't already exist in the source.
                setBand3Request(new SubmissionRequest(src.getBand3Request()));
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
            setTooOption(src.getTooOption());
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }
    }

    public QueueProposal(final ClassicalProposal src){
        super(src, CloneMode.FULL);
        setTooOption(src.getTooOption()); //Always NONE
        band3Request = new SubmissionRequest();
        band3Request.setMinTime(src.getTotalMinRequestedTime());
        band3Request.setTime(src.getTotalRequestedTime());
    }

    public QueueProposal(final SubmissionRequest band3Request, final TooOption tooOption) {
        this.band3Request = band3Request;
        this.tooOption = tooOption;
    }

    public QueueProposal(final Proposal mProposal, final QueueProposalClass queueProposalClass, List<Partner> partners) {
        super(mProposal, partners);

        setTooOption(queueProposalClass.getTooOption());
        setComment(queueProposalClass.getComment());
        setSubmissionsKey(queueProposalClass.getKey());

        if (queueProposalClass.getBand3Request() != null) {
            setBand3Request(new SubmissionRequest(queueProposalClass.getBand3Request()));
        }

        if (queueProposalClass.getItac() != null) {
            setItac(new edu.gemini.tac.persistence.phase1.Itac(queueProposalClass.getItac()));
        }
    }

    @Override
    public String toString() {
        return "QueueProposal{" +
                "band3Request=" + band3Request +
                ", tooOptions=" + tooOption +
                '}';
    }

    @Override
    public edu.gemini.model.p1.mutable.Proposal toMutable(HashMap<Investigator, String> hibernateInvestigatorToIdMap) {
        final edu.gemini.model.p1.mutable.Proposal proposal = new edu.gemini.model.p1.mutable.Proposal();
        final edu.gemini.model.p1.mutable.ProposalClassChoice proposalClassChoice = new edu.gemini.model.p1.mutable.ProposalClassChoice();
        final edu.gemini.model.p1.mutable.QueueProposalClass queueProposalClass = new edu.gemini.model.p1.mutable.QueueProposalClass();

        proposal.setProposalClass(proposalClassChoice);
        proposalClassChoice.setQueue(queueProposalClass);

        queueProposalClass.setComment(getComment());
        queueProposalClass.setItac(getParent().getItac().toMutable());
        queueProposalClass.setKey(getSubmissionsKey());

        queueProposalClass.setTooOption(tooOption);

        if (band3Request != null) {
            queueProposalClass.setBand3Request(band3Request.toMutable());
        }

        for (NgoSubmission ngo : ngos) {
            final SubmissionRequest request = ngo.getRequest();
            final Investigator partnerLead = ngo.getPartnerLead();
            String partnerLeadInvestigatorId = hibernateInvestigatorToIdMap.get(partnerLead);
            edu.gemini.model.p1.mutable.NgoSubmission mNgoSubmission = null;
            if (partnerLeadInvestigatorId == null) {
                partnerLeadInvestigatorId = hibernateInvestigatorToIdMap.get(getInvestigators().getPi());
                mNgoSubmission = ngo.toMutable(getInvestigators().getPi(), partnerLeadInvestigatorId);
            } else {
                mNgoSubmission = ngo.toMutable(ngo.getPartnerLead(), partnerLeadInvestigatorId);
            }

            queueProposalClass.getNgo().add(mNgoSubmission);
        }


        Collections.sort(queueProposalClass.getNgo(), new Comparator<edu.gemini.model.p1.mutable.NgoSubmission>() {
            @Override
            public int compare(edu.gemini.model.p1.mutable.NgoSubmission left, edu.gemini.model.p1.mutable.NgoSubmission right) {
                if (left.getResponse() == null || right.getResponse() == null)
                    return 0;
                if (left.getResponse().getReceipt() == null || right.getResponse().getReceipt() == null)
                    return 0;

                return left.getResponse().getReceipt().getId().compareTo(right.getResponse().getReceipt().getId());
            }
        });

        if (exchange != null) {
            final edu.gemini.model.p1.mutable.ExchangeSubmission mExchangeSubmission = exchange.toMutable(exchange.getPartnerLead(), hibernateInvestigatorToIdMap.get(exchange.getPartnerLead()));

            queueProposalClass.setExchange(mExchangeSubmission);
        }

        super.toMutable(proposal, hibernateInvestigatorToIdMap);

        return proposal;
    }

    @Override
    public boolean isClassical() {
        return false;
    }

    @Override
    public PhaseIProposal memberClone() {
        return new QueueProposal(this);
    }

    @Override
    public PhaseIProposal memberCloneLoaded() {
        return new QueueProposal(this, CloneMode.SWALLOW_EXCEPTIONS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueueProposal)) return false;
        if (!super.equals(o)) return false;

        QueueProposal that = (QueueProposal) o;

        if (band3Request != null ? !band3Request.equals(that.band3Request) : that.band3Request != null) return false;
        if (tooOption != that.tooOption) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (band3Request != null ? band3Request.hashCode() : 0);
        result = 31 * result + (tooOption != null ? tooOption.hashCode() : 0);
        return result;
    }

    @Override
    public SubmissionRequest getBand3Request() {
        return band3Request;
    }

    public void setBand3Request(SubmissionRequest band3Request) {
        this.band3Request = band3Request;
    }

    @Override
    public TooOption getTooOption() {
        return tooOption;
    }

    public void setTooOption(TooOption tooOption) {
        this.tooOption = tooOption;
    }

    @Override
    public boolean isToo() {
        return !(getTooOption().equals(TooOption.NONE));
    }

    @Override
    public boolean isQueue() {
        return true;
    }
}
