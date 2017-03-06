package edu.gemini.tac.persistence.phase1.proposal;

import edu.gemini.model.p1.mutable.ExchangePartner;
import edu.gemini.model.p1.mutable.ExchangeProposalClass;
import edu.gemini.model.p1.mutable.Proposal;
import edu.gemini.model.p1.mutable.ProposalClassChoice;
import edu.gemini.tac.persistence.IValidateable;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.phase1.Investigator;
import edu.gemini.tac.persistence.phase1.Itac;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionRequest;
import org.apache.commons.lang.Validate;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@DiscriminatorValue("ExchangeProposal")
public class ExchangeProposal extends PhaseIProposal implements IValidateable {
    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_partner")
    protected ExchangePartner partner;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "phase_i_proposal_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Set<NgoSubmission> ngos = new HashSet<NgoSubmission>();

    public ExchangeProposal() {}
    public ExchangeProposal(final ExchangeProposal src) {
        this(src, CloneMode.FULL);
    }

    public ExchangeProposal(final ExchangeProposal src, final CloneMode cloneMode) {
        super(src, cloneMode);

        try {
            setPartner(src.getPartner());
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
            for (NgoSubmission ngoSubmission : src.getNgos())
                ngos.add(new NgoSubmission(ngoSubmission));
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }
    }


    public ExchangeProposal(final Proposal mp, final ExchangeProposalClass exchangeProposalClass, List<Partner> partners) {
        super(mp, partners);

        this.partner = exchangeProposalClass.getPartner();

        for (edu.gemini.model.p1.mutable.NgoSubmission s : exchangeProposalClass.getNgo()) {
            getNgos().add(new NgoSubmission(s, converter.investigatorMap, converter.partnerMap));
        }

        setSubmissionsKey(exchangeProposalClass.getKey());
        setComment(exchangeProposalClass.getComment());

        if (exchangeProposalClass.getItac() != null)
            setItac(new Itac(exchangeProposalClass.getItac()));
    }

    @Override
    public Site getSite() {
        // NOTE: maybe this will have to be revisited in the future; currently only
        // gemini normal proposals belong to a site (either North or South), maybe
        // we want to define a Keck and a Subaru site or we want to make sure that
        // ExchangeProposals are not mixed with GeminiNormalProposals in places
        // where the site is relevant.
        //throw new RuntimeException("Exchange proposals don't belong to a site.");
        return null;
    }

    @Override
    public String toString() {
        return "ExchangeProposal{" +
                "partner=" + partner +
                ", ngo=" + ngos +
                "} " + super.toString();
    }

    @Override
    public Submission getPrimary() {
        if (primarySubmission != null)
            return primarySubmission;

        final List<NgoSubmission> accepted = new ArrayList<NgoSubmission>();
        for (NgoSubmission s : ngos) {
            if (s.getAccept() != null)
                accepted.add(s);
        }

        Validate.isTrue(!(accepted.size() > 1), "Invalid data for submissions choice in " + this.getClass().getSimpleName() + ".  Multiple accepted ngos contained.");
        Validate.isTrue(!(accepted.size() < 1), "Invalid data for submissions choice in " + this.getClass().getSimpleName() + ".  No accepted ngos contained.");

        return accepted.get(0); // THERE CAN BE ONLY ONE.
    }

    @Override
    public List<Submission> getSubmissions() {
        return new ArrayList<Submission>(ngos);
    }

    @Override
    public void addSubmission(final Submission s) {
        Validate.isTrue(s instanceof NgoSubmission);
        ngos.add((NgoSubmission) s);
    }

    @Override
    public void clearSubmissions() {
        ngos.clear();
    }

    @Override
    public void hibernateInitializeSubmissions() {
        Hibernate.initialize(ngos);
    }

    @Override
    public Proposal toMutable(HashMap<Investigator, String> hibernateInvestigatorToIdMap) {
        final Proposal proposal = new Proposal();
        final ProposalClassChoice proposalClassChoice = new ProposalClassChoice();
        final ExchangeProposalClass exchangeProposalClass = new ExchangeProposalClass();

        proposal.setProposalClass(proposalClassChoice);
        proposalClassChoice.setExchange(exchangeProposalClass);
        exchangeProposalClass.setComment(getComment());
        exchangeProposalClass.setItac(getItac().toMutable());
        exchangeProposalClass.setKey(getSubmissionsKey());
        exchangeProposalClass.setPartner(partner);

        for (NgoSubmission ngo : ngos) {
            final Investigator partnerLead = ngo.getPartnerLead();
            String partnerLeadInvestigatorId = hibernateInvestigatorToIdMap.get(partnerLead);
            edu.gemini.model.p1.mutable.NgoSubmission mNgoSubmission = null;
            if (partnerLeadInvestigatorId == null) {
                partnerLeadInvestigatorId = hibernateInvestigatorToIdMap.get(getInvestigators().getPi());
                mNgoSubmission = ngo.toMutable(getInvestigators().getPi(), partnerLeadInvestigatorId);
            } else {
                mNgoSubmission = ngo.toMutable(ngo.getPartnerLead(), partnerLeadInvestigatorId);
            }
            exchangeProposalClass.getNgo().add(mNgoSubmission);
        }

        super.toMutable(proposal, hibernateInvestigatorToIdMap);

        return proposal;
    }

    @Override
    public PhaseIProposal memberClone() {
        return new ExchangeProposal(this);
    }

    @Override
    public PhaseIProposal memberCloneLoaded() {
        return new ExchangeProposal(this, CloneMode.SWALLOW_EXCEPTIONS);
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
        Validate.notNull(getPrimary());
        Validate.notNull(getPrimary().getAccept()); // assumption: primary always has an accept
        return getPrimary().getAccept().getRecommend();
    }

    @Override
    public TimeAmount getTotalMinRecommendedTime() {
        Validate.notNull(getPrimary());
        Validate.notNull(getPrimary().getAccept()); // assumption: primary always has an accept
        return getPrimary().getAccept().getMinRecommend();
    }

    public ExchangePartner getPartner() {
        return partner;
    }

    public void setPartner(ExchangePartner partner) {
        this.partner = partner;
    }

    public Set<NgoSubmission> getNgos() {
        return ngos;
    }

    public void setNgos(Set<NgoSubmission> ngo) {
        this.ngos = ngo;
    }

    @Override
    public void normalizeInvestigator(final Investigator replaceMe, final Investigator with) {
        super.normalizeInvestigator(replaceMe, with);

        // -- references in submissions/ngo/request@lead
        for (NgoSubmission ngo : getNgos()) {
            if (ngo.getPartnerLead().equals(replaceMe))
                ngo.setPartnerLead(with);
        }
    }

    public boolean isExchange() {
        return true;
    }
}
