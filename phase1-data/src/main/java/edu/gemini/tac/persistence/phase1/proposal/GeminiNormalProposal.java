package edu.gemini.tac.persistence.phase1.proposal;

import edu.gemini.model.p1.mutable.ClassicalProposalClass;
import edu.gemini.model.p1.mutable.Proposal;
import edu.gemini.model.p1.mutable.ProposalClassChoice;
import edu.gemini.model.p1.mutable.QueueProposalClass;
import edu.gemini.tac.persistence.IValidateable;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.submission.ExchangeSubmission;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import org.apache.commons.lang.Validate;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@NamedQueries({
          @NamedQuery(name="gemininormalproposal.findProposalsForQueueCreationByCommittee",
                query = "from Proposal p " +
                            "join fetch p.committee comm " +
                        "left outer join fetch p.phaseIProposal p1p " +
                                        //"join fetch p1p.observations os " +
                                        //"left outer join fetch p1p.investigators i " +
                                        "join fetch p1p.conditions " +
                                        "join fetch p.partner partner " +
                                        "join fetch p1p.itac itac " +
                                        "join fetch itac.accept " +
                                        //"join fetch p1p.submissions subs " +
                                        "left outer join fetch p.issues " +
                                        //"left outer join fetch subs.ngo ngos " +
                                        //"left outer join fetch subs.exchange x " +
                                        //"join fetch i.pi pi " +
                                        //"join fetch i.coi cois " +
                                        "where p.committee = :committeeId "

        )
})
abstract public class GeminiNormalProposal extends PhaseIProposal implements IValidateable {
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "phase_i_proposal_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Set<NgoSubmission> ngos = new HashSet<NgoSubmission>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "exchange_submission_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected ExchangeSubmission exchange;

    public GeminiNormalProposal() {}

    public GeminiNormalProposal(final GeminiNormalProposal src, final CloneMode cloneMode) {
        super(src, cloneMode);
        //Must have either an exchange or some NGO submissions
        try {
            Validate.isTrue(src.isExchange() || src.getNgos().size() > 0, "Submissions must have either an ExchangeSubmission or NGO submissions");
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
            if (src.isExchange()) {
                setExchange(new ExchangeSubmission(src.getExchange()));
            }
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
            if (src.getNgos().size() > 0) {
                for (NgoSubmission n : src.getNgos()) {
                    ngos.add(new NgoSubmission(n));
                }
            }
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }
    }

    public GeminiNormalProposal(final Proposal mProposal, List<Partner> partners) {
        super(mProposal, partners);

        ProposalClassChoice proposalClassChoice = mProposal.getProposalClass();
        Validate.isTrue(proposalClassChoice.getClassical() != null || proposalClassChoice.getQueue() != null);
        ClassicalProposalClass classicalProposalClass = proposalClassChoice.getClassical();
        QueueProposalClass queueProposalClass = proposalClassChoice.getQueue();

        edu.gemini.model.p1.mutable.ExchangeSubmission exchangeSubmission = null;
        List<edu.gemini.model.p1.mutable.NgoSubmission> ngoSubmissions = null;
        
        if (classicalProposalClass != null) {
            exchangeSubmission = classicalProposalClass.getExchange();
            ngoSubmissions = classicalProposalClass.getNgo();
        } else if (queueProposalClass != null) {
            exchangeSubmission = queueProposalClass.getExchange();
            ngoSubmissions = queueProposalClass.getNgo();
        } else {
            throw new RuntimeException("Forgot to update the validate check above.");
        }

        if (exchangeSubmission != null)
            setExchange(new ExchangeSubmission(exchangeSubmission, converter.investigatorMap, converter.partnerMap));

        if (ngoSubmissions != null) {
            for (edu.gemini.model.p1.mutable.NgoSubmission ms : ngoSubmissions) {
                getNgos().add(new NgoSubmission(ms, converter.investigatorMap, converter.partnerMap));
            }
        }
        
        Validate.isTrue(exchange != null || (ngoSubmissions != null && ngoSubmissions.size() > 0));
    }

    public boolean isExchange() {
        return getExchange() != null;
    }

    /**
     * A handy method for me to encode assumptions I'm making about what goes into a valid
     * model.  To be proved true or false, and modified as we go along.
     */
    @Override
    public void validate() {
        super.validate();
        if (ngos.size() == 0 && exchange == null)
            throw new IllegalStateException("A submissions choice must have either an NGO component or an exchange component.");
        if (ngos.size() > 0 && exchange != null)
            throw new IllegalStateException("A submissions choice must not have both an NGO component and an exchange component.");
    }

    @Override
    public String toString() {
        return "GeminiNormalProposal{" +
                "ngo=" + ngos +
                ", exchange=" + exchange +
                '}';
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

    public void setNgos(Set<NgoSubmission> ngos) {
        this.ngos = ngos;
    }

    public void setExchange(ExchangeSubmission exchange) {
        this.exchange = exchange;
    }
    public Set<NgoSubmission> getNgos() {
        return ngos;
    }

    public ExchangeSubmission getExchange() {
        return exchange;
    }

    public Submission getPrimary() {
        if (primarySubmission != null)
            return primarySubmission;

        if (exchange != null)
            return exchange;

        final List<NgoSubmission> accepted = new ArrayList<NgoSubmission>();
        for (NgoSubmission s : ngos) {
            if (s.getAccept() != null)
                accepted.add(s);
        }

        Validate.isTrue(!(accepted.size() > 1), "Invalid data for submissions choice in " + this.getClass().getSimpleName() + ".  Multiple accepted ngos contained.");
        Validate.isTrue(!(accepted.size() < 1), "Invalid data for submissions choice in " + this.getClass().getSimpleName() + ".  No accepted ngos contained.");

        return accepted.get(0); // THERE CAN BE ONLY ONE.
    }

    public String toDot(){
        String myDot = "NgoSubmission";
        if(isExchange()){
            myDot = "ExchangeSubmission";
        }

        String dot = myDot + ";\n";
        if(isExchange()){
            dot += myDot + "->" + getExchange().toDot() + ";\n";
        }else{
            for(NgoSubmission ng : ngos){
                dot += myDot + "->" + ng.toDot() + ";\n";
            }
        }
        dot += myDot + "->" + itac.toDot() + ";\n";
        return dot;
    }

    @Override
    public void hibernateInitializeSubmissions() {
        Hibernate.initialize(ngos);
        Hibernate.initialize(exchange);
        if (exchange != null) {
            Hibernate.initialize(exchange.getPartnerLead());
        }
    }

    @Override
    public List<Submission> getSubmissions() {
        final ArrayList<Submission> copy = new ArrayList<Submission>(ngos);
        if (exchange != null)
            copy.add(exchange);

        return Collections.unmodifiableList(copy);
    }

    @Override
    public void clearSubmissions() {
        ngos.clear();
    }

    @Override
    public void addSubmission(final Submission s) {
        Validate.isTrue(s instanceof NgoSubmission || s instanceof ExchangeSubmission);
        if (s instanceof NgoSubmission)
            ngos.add((NgoSubmission) s);
        else
            exchange = (ExchangeSubmission) s;
    }

    @Override
    public void normalizeInvestigator(final Investigator replaceMe, final Investigator with) {
        super.normalizeInvestigator(replaceMe, with);

        // -- references in submissions/ngo/request@lead
        for (NgoSubmission ngo : getNgos()) {
            if (ngo.getPartnerLead().equals(replaceMe))
                ngo.setPartnerLead(with);
        }

        // -- references in exchange part
        if (getExchange() != null) {
            if (getExchange().getPartnerLead().equals(replaceMe)) {
                getExchange().setPartnerLead(with);
            }
        }

    }
}