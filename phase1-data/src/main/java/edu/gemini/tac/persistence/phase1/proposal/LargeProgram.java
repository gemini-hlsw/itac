package edu.gemini.tac.persistence.phase1.proposal;

import edu.gemini.model.p1.mutable.*;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.Investigator;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.submission.LargeProgramSubmission;
import edu.gemini.tac.persistence.phase1.submission.SpecialSubmission;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionRequest;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A Gemini large program has a large program submission that doesn't go to partners.
 * Only Gemini instruments may be used for proposals of this type; note that this check is
 * independent of schema validation.
 */
@Entity
@DiscriminatorValue("LargeProgram")
public class LargeProgram extends PhaseIProposal {
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "special_submission_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected LargeProgramSubmission largeProgramSubmission;

    @Column(name = "too_option")
    @Enumerated(EnumType.STRING)
    protected TooOption tooOption;

    protected LargeProgram() {}

    public LargeProgram(TooOption tooOption) {
        this.tooOption = tooOption;
    }

    public LargeProgram(final Proposal mp, final LargeProgramClass largeProgramClass, final List<Partner> partners) {
        super(mp, partners);

        Partner lpPartner = null;
        for (Partner p: partners) {
            if (p.isLargeProgram()) {
                lpPartner = p;
                break;
            }
        }
        setPrimarySubmission(new LargeProgramSubmission(largeProgramClass.getSubmission(), lpPartner));

        setSubmissionsKey(largeProgramClass.getKey());
        setComment(largeProgramClass.getComment());
        setTooOption(largeProgramClass.getTooOption());

        if (largeProgramClass.getItac() != null)
            setItac(new edu.gemini.tac.persistence.phase1.Itac(largeProgramClass.getItac()));
    }

    public LargeProgram(final LargeProgram src) { this(src, CloneMode.FULL); }

    public LargeProgram(final LargeProgram src, final PhaseIProposal.CloneMode cloneMode) {
        super(src, cloneMode);

        try {
            if (src.getPrimary() != null) // Don't create a band 3 request in the copy if one doesn't already exist in the source.
                setPrimarySubmission(new LargeProgramSubmission(src.getPrimary().getPartner(), src.getPrimary().getRequest(), src.getPrimary().getReceipt(), src.getPrimary().getAccept(), src.getPrimary().getRejected()));
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

    @Override
    public TimeAmount getTotalRequestedTime() {
        return largeProgramSubmission.getRequest().getTime();
    }

    @Override
    public TimeAmount getTotalMinRequestedTime() {
        return largeProgramSubmission.getRequest().getMinTime();
    }

    @Override
    public TimeAmount getTotalRecommendedTime() {
        return largeProgramSubmission.getAccept().getRecommend();
    }

    @Override
    public TimeAmount getTotalMinRecommendedTime() {
        return largeProgramSubmission.getAccept().getMinRecommend();
    }

    public void setPrimarySubmission(final LargeProgramSubmission s) {
        this.largeProgramSubmission = s;
    }

    @Override
    public Submission getPrimary() {
        return largeProgramSubmission;
    }

    @Override
    public List<Submission> getSubmissions() {
        List<Submission> submission = new ArrayList<Submission>();
        submission.add(largeProgramSubmission);
        return Collections.unmodifiableList(submission);
    }

    @Override
    public void addSubmission(Submission s) {

    }

    @Override
    public void clearSubmissions() {

    }

    @Override
    public void hibernateInitializeSubmissions() {
        Hibernate.initialize(largeProgramSubmission);
    }

    @Override
    public Proposal toMutable(HashMap<Investigator, String> hibernateInvestigatorToIdMap) {
        final Proposal proposal = new Proposal();
        final ProposalClassChoice proposalClassChoice = new ProposalClassChoice();
        final LargeProgramClass proposalClass = new LargeProgramClass();

        proposal.setProposalClass(proposalClassChoice);
        proposalClassChoice.setLarge(proposalClass);

        proposalClass.setComment(getComment());
        proposalClass.setItac(getItac().toMutable());
        proposalClass.setKey(getSubmissionsKey());
        proposalClass.setTooOption(tooOption);
        proposalClass.setSubmission(largeProgramSubmission.toMutable());

        super.toMutable(proposal, hibernateInvestigatorToIdMap);

        return proposal;
    }

    @Override
    public PhaseIProposal memberClone() {
        return new LargeProgram(this);
    }

    @Override
    public PhaseIProposal memberCloneLoaded() {
        return null;
    }

    @Override
    public boolean isLargeProgram() {
        return true;
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


}
