package edu.gemini.tac.persistence.phase1.proposal;

import edu.gemini.model.p1.mutable.ClassicalProposalClass;
import edu.gemini.model.p1.mutable.Proposal;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.Investigator;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionRequest;
import org.apache.commons.lang.Validate;
import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.*;

@Entity
@DiscriminatorValue("ClassicalProposal")
public class ClassicalProposal extends GeminiNormalProposal {
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "v2_phase_i_proposals_classical_investigators",
            joinColumns = @JoinColumn(name = "phase_i_proposal_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "investigators_id", referencedColumnName = "id")
    )
    @Fetch(FetchMode.SUBSELECT)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Set<Investigator> classicalVisitors = new HashSet<Investigator>();

    public ClassicalProposal() {}
    public ClassicalProposal(final ClassicalProposal src) { this(src, CloneMode.FULL); }
    public ClassicalProposal(final ClassicalProposal src, final CloneMode cloneMode) {
        super(src, cloneMode);

        try {
            classicalVisitors.addAll(src.getClassicalVisitors());
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }
    }

    public ClassicalProposal(final GeminiNormalProposal src) {
        super(src, CloneMode.FULL);
    }

    public ClassicalProposal(final Proposal mProposal, final ClassicalProposalClass classicalProposalClass, final List<Partner> partners) {
        super(mProposal, partners);

        for (edu.gemini.model.p1.mutable.Visitor v : classicalProposalClass.getVisitor()) {
            final String visitorId = v.getRef().getId();
            classicalVisitors.add(converter.investigatorMap.get(visitorId));
        }
        
        setSubmissionsKey(classicalProposalClass.getKey());
        setComment(classicalProposalClass.getComment());

        if (classicalProposalClass.getItac() != null)
            setItac(new edu.gemini.tac.persistence.phase1.Itac(classicalProposalClass.getItac()));
    }

    public Set<Investigator> getClassicalVisitors() {
        return classicalVisitors;
    }

    public void setClassicalVisitors(Set<Investigator> classicalVisitors) {
        this.classicalVisitors = classicalVisitors;
    }

    @Override
    public edu.gemini.model.p1.mutable.Proposal toMutable(HashMap<Investigator, String> hibernateInvestigatorToIdMap) {
        final edu.gemini.model.p1.mutable.Proposal proposal = new edu.gemini.model.p1.mutable.Proposal();
        final edu.gemini.model.p1.mutable.ProposalClassChoice proposalClassChoice = new edu.gemini.model.p1.mutable.ProposalClassChoice();
        final edu.gemini.model.p1.mutable.ClassicalProposalClass classicalProposalClass = new edu.gemini.model.p1.mutable.ClassicalProposalClass();

        proposal.setProposalClass(proposalClassChoice);
        proposalClassChoice.setClassical(classicalProposalClass);

        classicalProposalClass.setComment(getComment());
        classicalProposalClass.setItac(getParent().getItac().toMutable());
        classicalProposalClass.setKey(getSubmissionsKey());

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
            classicalProposalClass.getNgo().add(mNgoSubmission);
        }

        Collections.sort(classicalProposalClass.getNgo(), new Comparator<edu.gemini.model.p1.mutable.NgoSubmission>() {
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
            classicalProposalClass.setExchange(mExchangeSubmission);
        }

        super.toMutable(proposal, hibernateInvestigatorToIdMap);

        for (Investigator i: classicalVisitors) {
            final edu.gemini.model.p1.mutable.Visitor visitor = new edu.gemini.model.p1.mutable.Visitor();
            final String investigatorReferenceId = hibernateInvestigatorToIdMap.get(i);

            if (proposal.getInvestigators().getPi().getId().equals(investigatorReferenceId)) {
                visitor.setRef(proposal.getInvestigators().getPi());
            } else {
                for (edu.gemini.model.p1.mutable.Investigator mi : proposal.getInvestigators().getCoi()) {
                    if (mi.getId().equals(investigatorReferenceId))
                        visitor.setRef(mi);
                }
            }

            if (visitor.getRef() != null) {
                classicalProposalClass.getVisitor().add(visitor);
            } else {
                final StringBuilder stringBuilder = new StringBuilder("toMutable failed matching investigators to visitors.");
                stringBuilder.append("classicalVisitors:" + classicalVisitors);
                stringBuilder.append("hibernateInvestigatorToIdMap:" + hibernateInvestigatorToIdMap);
                throw new IllegalStateException(stringBuilder.toString());
            }
        }

        return proposal;
    }

    @Override
    public boolean isClassical() {
        return true;
    }

    @Override
    public PhaseIProposal memberClone() {
        return new ClassicalProposal(this);
    }

    @Override
    public PhaseIProposal memberCloneLoaded() {
        return new ClassicalProposal(this, CloneMode.SWALLOW_EXCEPTIONS);
    }

    @Override
    public void normalizeInvestigator(final Investigator replaceMe, final Investigator with) {
        super.normalizeInvestigator(replaceMe, with);

        // -- references in scheduling/classical/visitor@ref
        if (getClassicalVisitors().contains(replaceMe)) {
            getClassicalVisitors().remove(replaceMe);
            getClassicalVisitors().add(with);
        }
    }
}
