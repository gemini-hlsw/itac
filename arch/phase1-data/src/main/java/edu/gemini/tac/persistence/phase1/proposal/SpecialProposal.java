package edu.gemini.tac.persistence.phase1.proposal;

import edu.gemini.model.p1.mutable.NgoSubmission;
import edu.gemini.model.p1.mutable.Proposal;
import edu.gemini.model.p1.mutable.ProposalClassChoice;
import edu.gemini.model.p1.mutable.SpecialProposalClass;
import edu.gemini.tac.persistence.IValidateable;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.Investigator;
import edu.gemini.tac.persistence.phase1.Itac;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.submission.SpecialSubmission;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;
import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A Gemini special proposal has a type and a special submission that doesn't go to partners.
 * Only Gemini instruments may be used for proposals of this type; note that this check is
 * independent of schema validation.
 */
@Entity
@DiscriminatorValue("SpecialProposal")
public class SpecialProposal extends PhaseIProposal implements IValidateable {
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "special_submission_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected SpecialSubmission specialSubmission;

    public SpecialProposal(){}
    public SpecialProposal(final SpecialProposal src) { this(src, CloneMode.FULL); }
    public SpecialProposal(final SpecialProposal src, final CloneMode cloneMode) {
        super(src, cloneMode);

        try{
            setSpecialSubmission(new SpecialSubmission(src.getSpecialSubmission()));
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }
    }

    public SpecialProposal(final Proposal mp, final SpecialProposalClass specialProposalClass, final List<Partner> partners) {
        super(mp, partners);

        setSpecialSubmission(new SpecialSubmission(specialProposalClass.getSubmission()));

        setSubmissionsKey(specialProposalClass.getKey());
        setComment(specialProposalClass.getComment());

        if (specialProposalClass.getItac() != null)
            setItac(new Itac(specialProposalClass.getItac()));
    }

    @Override
    public Submission getPrimary() {
        validate();

        return specialSubmission;
    }

    @Override
    public TimeAmount getTotalRequestedTime() {
        TimeAmount totalTime = new TimeAmount(BigDecimal.ZERO, TimeUnit.HR);
        for (Submission s : getSubmissions()) {
            Validate.notNull(s.getRequest()); // assumption: all submissions always have a request

            totalTime = totalTime.sum(s.getRequest().getTime());
        }

        return totalTime;
    }

    @Override
    public TimeAmount getTotalMinRequestedTime() {
        TimeAmount totalTime = new TimeAmount(BigDecimal.ZERO, TimeUnit.HR);

        for (Submission s : getSubmissions()) {
            Validate.notNull(s.getRequest()); // assumption: all submissions always have a request
            totalTime = totalTime.sum(s.getRequest().getMinTime());
        }

        return totalTime;
    }

    @Override
    public TimeAmount getTotalRecommendedTime() {
        return specialSubmission.getAccept().getRecommend();
    }

    @Override
    public TimeAmount getTotalMinRecommendedTime() {
        return specialSubmission.getAccept().getMinRecommend();
    }

    @Override
    public String toString() {
        return "SpecialProposal{" +
                "specialSubmission=" + specialSubmission +
                '}';
    }

    public SpecialSubmission getSpecialSubmission() {
        return specialSubmission;
    }

    public void setSpecialSubmission(SpecialSubmission specialSubmission) {
        this.specialSubmission = specialSubmission;
    }

    @Override
    public List<Submission> getSubmissions() {
        final ArrayList<Submission> submissions = new ArrayList<Submission>();
        submissions.add(specialSubmission);

        return submissions;
    }

    @Override
    public void addSubmission(final Submission s) {
        Validate.isTrue(s instanceof SpecialSubmission);
        specialSubmission = (SpecialSubmission) s;
    }

    @Override
    public void clearSubmissions() {
        specialSubmission = null;
    }

    @Override
    public void hibernateInitializeSubmissions() {
        
    }

    @Override
    public Proposal toMutable(HashMap<Investigator, String> hibernateInvestigatorToIdMap) {
        final Proposal proposal = new Proposal();
        final ProposalClassChoice proposalClassChoice = new ProposalClassChoice();
        final SpecialProposalClass proposalClass = new SpecialProposalClass();

        proposal.setProposalClass(proposalClassChoice);
        proposalClassChoice.setSpecial(proposalClass);

        proposalClass.setComment(getComment());
        proposalClass.setItac(getItac().toMutable());
        proposalClass.setKey(getSubmissionsKey());
        proposalClass.setSubmission(specialSubmission.toMutable());

        super.toMutable(proposal, hibernateInvestigatorToIdMap);

        return proposal;

    }

    @Override
    public PhaseIProposal memberClone() {
        return new SpecialProposal(this);
    }

    @Override
    public PhaseIProposal memberCloneLoaded() {
        return new SpecialProposal(this, CloneMode.SWALLOW_EXCEPTIONS);
    }

    @Override
    public void validate() {
        super.validate();

        if (specialSubmission == null) {
            throw new IllegalStateException("Special proposal with no set type.");
        }
    }
}
