package edu.gemini.tac.persistence.phase1.proposal;

import edu.gemini.model.p1.mutable.*;
import edu.gemini.tac.persistence.IValidateable;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.Investigator;
import edu.gemini.tac.persistence.phase1.Investigators;
import edu.gemini.tac.persistence.phase1.Itac;
import edu.gemini.tac.persistence.phase1.Meta;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.Target;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TooTarget;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.util.MutableToHibernateConverter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Fetch;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.*;

/**
 * Hibernate Annotation is still incomplete.
 * Equals and hashCode (badly) implemented.
 */
@NamedQueries({
    @NamedQuery(name = "phaseIProposal.findById", query = "from PhaseIProposal where id = :id"),
    @NamedQuery(name = "phaseIProposal.findClassicalProposalById", query = "from ClassicalProposal where id = :id"),
    @NamedQuery(name = "phaseIProposal.findExchangeProposalById", query = "from ExchangeProposal where id = :id"),
    @NamedQuery(name = "phaseIProposal.findQueueProposalById", query = "from QueueProposal where id = :id")
})
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Table(name = "v2_phase_i_proposals")
public abstract class PhaseIProposal implements IValidateable, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    protected Proposal parent;

    @Embedded
    protected Meta meta;

    @Column(name = "title", nullable = false)
    protected String title;

    @Column(name = "abstract", nullable = false)
    protected String _abstract;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "v2_keywords",
            joinColumns = @JoinColumn(name = "v2_phase_i_proposal_id"))
    @Column(name = "keyword")
    @Enumerated(EnumType.STRING)
    @Fetch(value = FetchMode.SUBSELECT)
    protected Set<Keyword> keywords = new HashSet<>();

    @Embedded
    protected Investigators investigators = new Investigators();

    @OneToMany(mappedBy = "phaseIProposal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    @OrderBy("targetId")
    @Fetch(value = FetchMode.SUBSELECT)
    protected List<Target> targets = new ArrayList<>();

    @OneToMany(mappedBy = "phaseIProposal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    @OrderBy("conditionId")
    @Fetch(value = FetchMode.SUBSELECT)
    protected List<Condition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "phaseIProposal",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE, org.hibernate.annotations.CascadeType.REMOVE, org.hibernate.annotations.CascadeType.DELETE_ORPHAN})
    @OrderBy("blueprintId")
    @Fetch(value = FetchMode.SUBSELECT)
    protected List<BlueprintBase> blueprints = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "proposal")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Set<Observation> observations = new HashSet<>();

    @Column(name = "schema_version", nullable = false)
    protected String schemaVersion;

    @Column(name = "tac_category")
    @Enumerated(EnumType.STRING)
    protected TacCategory tacCategory;

    // Located in ProposalClass element from schema.
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "itac_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Itac itac = new Itac();

    @Column(name = "comment")
    protected String comment;

    @Column(name = "submissions_key")
    protected String submissionsKey;

    @Column(name = "scheduling")
    protected String scheduling = "";

    /**
     * Allows creator of derived phase I proposals to manually specify which submission is master rather than
     * scanning for the accept.
     */
    @Transient
    protected Submission primarySubmission = null;

    /**
     * Used to aid mapping between mutable XML ids and persistent objects.  Will usually be null, will never
     * be persisted.
     */
    @Transient
    protected MutableToHibernateConverter converter;

    /**
     * When cloning, because of the way joint proposals are being handled, we create a transient instance.  We want
     * to be able to succinctly clone the information that is there without constraining ourselves to pull the entire
     * proposal from the database.
     */
    protected enum CloneMode {
        SWALLOW_EXCEPTIONS, // Best efforts, clone what's there.
        FULL // Clone all the things
    }

    protected PhaseIProposal () {}

    public PhaseIProposal(final PhaseIProposal src) {
        this(src, CloneMode.FULL);
    }

    /**
     * See enum CloneMode for a brief discussion of the following ugly code.
     *
     * @param src proposal to copy.
     * @param cloneMode SWALLOW_EXCEPTIONS will not throw if a proxy object or collection is unable to be copied, but
     *                  instead moves along.  FULL will take everything and therefore requires everything be
     *                  instantiated
     */
    public PhaseIProposal(final PhaseIProposal src, final CloneMode cloneMode) {
        try {
            setMeta(new Meta(src.getMeta()));
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
            for (BlueprintBase b: src.getBlueprints()) {
                // In order to not have to write even more factory methods and/or copy constructors
                // I take the elegant, quick and somewhat dirty toMutable->fromMutable detour.
                BlueprintBase copy = BlueprintBase.fromMutable(b.toMutable().getBlueprintChoice());
                if(b.getBlueprintId() != null){
                    copy.setBlueprintId(b.getBlueprintId());
                }else{
                    copy.setBlueprintId("");
                }
                copy.setPhaseIProposal(this);               // important: re-parent the blueprint clone
                blueprints.add(copy);
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
            setTitle(src.getTitle());
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
            setProposalAbstract(src.getProposalAbstract());
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }
        
        try {
            setSchemaVersion(src.getSchemaVersion());
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }


        try {
            setInvestigators(new Investigators(src.getInvestigators()));
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
            getKeywords().addAll(src.getKeywords());
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
            // comment
            for (Target t: src.getTargets()) {
                //
                Target clone = t.copy();
                clone.setPhaseIProposal(this);              // important: re-parent the target clone
                targets.add(clone);
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
            for (Condition c: src.getConditions()) {
                conditions.add(new Condition(c, this));     // important: re-parent the conditions clone
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
            for (Observation o: src.getObservations()) {
                observations.add(new Observation(o, this));
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
            setSubmissionsKey(generateNewSubmissionsKey());
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
           setItac(new Itac(src.getItac()));
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
            setComment(src.getComment());
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
            setTacCategory(src.getTacCategory());
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }

        try {
            setSubmissionsKey(src.getSubmissionsKey());     // keep the submission key
            setTacCategory(src.getTacCategory());
            setScheduling(src.getScheduling());
        } catch (LazyInitializationException lie) {
            switch(cloneMode) {
                case FULL:
                    throw lie;
                case SWALLOW_EXCEPTIONS:
                    // Continue on.
            }
        }
    }

    public PhaseIProposal(final edu.gemini.model.p1.mutable.Proposal mProposal, List<Partner> partners) {
        converter = new MutableToHibernateConverter(partners);
        converter.toHibernate(mProposal, this);
    }

    public static String generateNewSubmissionsKey() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhaseIProposal)) return false;

        PhaseIProposal that = (PhaseIProposal) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "PhaseIProposal{" +
                "id=" + id +
                ", meta=" + meta +
                ", title='" + title + '\'' +
                ", _abstract='" + _abstract + '\'' +
                ", keywords=" + keywords +
                ", investigators=" + investigators +
                ", targets=" + targets +
                ", conditions=" + conditions +
                ", observations=" + observations +
                ", schemaVersion='" + schemaVersion + '\'' +
                ", tacCategory=" + tacCategory +
                ", submissionsKey='" + submissionsKey + '\'' +
                ", itac=" + itac +
                ", comment='" + comment + '\'' +
                ", scheduling='" + scheduling +'\'' +
                '}';
    }

    /**
     * Mutates _this_ so that all blueprints are for instruments at the specific Site
     *
     * Blueprints that are already at the specified site are not mutated.  TODO: We shouldn't see any more of this
     * now that proposals are split between sites on import.  Time to simplify the code.
     *
     * Blueprints that are not at the specified site are removed from the proposal
     *
     * @param siteToSwitchTo New site for proposal.
     */
    public void switchTo(final Site siteToSwitchTo){
        //Use accumulator collections to avoid concurrent modification issues
        List<BlueprintBase> addThese = new ArrayList<>();
        for(BlueprintBase b : getBlueprints()){
            Validate.isTrue(b.getSite() != siteToSwitchTo);
            BlueprintBase c = b.getComplementaryInstrumentBlueprint();
            c.setPhaseIProposal(this);
            //Validate that the complementary instrument is at the other site
            Validate.isTrue(c.getSite() == siteToSwitchTo, "Blueprint \"" + c.getName() + "\" not at " + siteToSwitchTo.getDisplayName() + " as expected");
            addThese.add(c);

            for (Observation o : getObservations()) {
                if (b == o.getBlueprint()) {
                    o.setBlueprint(c);
                }
            }
            //Now null-out Observations in to-be-deleted BlueprintBase to avoid cascade re-save from Hibernate
            b.setObservations(new HashSet<>());
        }
        getBlueprints().clear();
        getBlueprints().addAll(addThese);
    }

    public String getTitle() {
        return title;
    }
    
    public String getReceiptId() {
        return getPrimary().getReceipt().getReceiptId();
    }

    public String getProposalAbstract() {
        return _abstract;
    }

    public Set<Keyword> getKeywords() {
        return keywords;
    }

    public Investigators getInvestigators() {
        return investigators;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<BlueprintBase> getBlueprints() {
        return blueprints;
    }

    public void setBlueprints(List<BlueprintBase> blueprints) {
        this.blueprints = blueprints;
    }

    /**
     *
     * @return all observations, active and inactive
     */
    public Set<Observation> getAllObservations() {
        return observations;
    }

    /**
     * @return Only active observations.
     */
    public Set<Observation> getObservations() {
        final Set<Observation> activeObservations = new HashSet<>();

        for (Observation o : observations) {
            if (o.getActive())
                activeObservations.add(o);
        }

        return Collections.unmodifiableSet(activeObservations);
    }

    public edu.gemini.tac.persistence.phase1.submission.SubmissionRequest getBand3Request() {
        return null;
    }

    /**
       Returns true if _any_ observation is in band3. N.B.: Even if B3 observations are all inactive!
       TODO: Cannot be changed to b3 "active aware" until cloneProposalForSite() is also modified
     */
    public boolean isBand3() {
        for (Observation o: observations)
            if (o.getBand() == Band.BAND_3)
                return true;

        return false;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public Long getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setProposalAbstract(String proposalAbstract) {
        this._abstract = proposalAbstract;
    }

    public void setKeywords(Set<Keyword> keywords) {
        this.keywords = keywords;
    }

    public void setInvestigators(Investigators investigators) {
        this.investigators = investigators;
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public void setObservations(Set<Observation> observations) {
        this.observations = observations;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Meta getMeta() {
        return meta;
    }


    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public String toDot() {
        String myDot = "PhaseIProposal_"  +  ((id == null) ? "NULL" : id.toString());
        String dot = myDot + ";\n";
        if(targets != null && targets.size() > 0){
            for(Target t : targets){
                dot += myDot + "->" + t.toDot() + ";\n";
            }
        }
        if(observations != null && observations.size() > 0){
            for(Observation o : observations){
                dot += myDot + "->" + o.toDot() + ";\n";
            }
        }
        if(conditions != null && conditions.size() > 0){
            for(Condition c : conditions){
                dot += myDot + "->" + c.toDot() + ";\n";
            }
        }
        return dot;
    }

    public void validate() {
        Validate.notEmpty(getBlueprints(), "Phase 1 proposal must have at least one blueprint.");
        Validate.notEmpty(getConditions(), "Phase 1 proposal  must have at least one condition.");
        Validate.notEmpty(getTargets(), "Phase 1 proposal  must have at least one target.");
        Validate.notEmpty(getAllObservations(), "Phase 1 proposal must have at least one observation.");
        for (BlueprintBase b : getBlueprints()) {
            b.validate();
        }
        for (Observation o : getObservations()) {
            o.validate();
            // TODO: this does not work with current equals implementation for blueprints/conditions/targets
//            Validate.isTrue(getBlueprints().contains(o.getBlueprint()), "Blueprints referenced by observations must be part of phase 1 proposal.");
//            Validate.isTrue(getConditions().contains(o.getCondition()), "Conditions referenced by observations must be part of phase 1 proposal.");
//            Validate.isTrue(getTargets().contains(o.getTarget()), "Targets referenced by observations must be part of phase 1 proposal.");
        }
        boolean hasNoneTooTargetsOnly = true;
        for (Target t: getTargets()) {
            if (t instanceof TooTarget) {
                hasNoneTooTargetsOnly = false;
            }
        }
        // too proposals can have too and non-too targets, but non-too proposals must not contain too targets!
        Validate.isTrue(isToo() || hasNoneTooTargetsOnly, "Non-too proposals must not contain too targets.");
    }

    /**
     * Returns the single Site for which this proposal is intended. (Used by "switch sites" use-case).
     *
     * When/If we support multi-site proposals, this will obviously have to go away
     *
     * @return the site that the proposal is associated with.  Implementation detail: this is determined from which
     *          blueprints are components.
     */
    public Site getSite(){
        Validate.isTrue(getBlueprints().size() > 0, "Trying to determine Site for phase i proposal [" + getId() +"]without blueprints.");
        BlueprintBase b = getBlueprints().get(0);
        return b.getSite();
    }

    /**
     * Gets all observations for instruments at the given site.
     * @param site filters observations to specific site.
     * 
     * @return observations that belong to the specified site.
     */
    public Set<Observation> getObservationsForSite(Site site) {
        Set<Observation> obs = new HashSet<>();
        for (Observation o : getObservations()) {
            if (o.getBlueprint().getSite() == site) {
                obs.add(o);
            }
        }
        return obs;
    }

    /**
     * Gets all targets for observations at the given site.
     * @param site
     * @return
     */
    public Set<Target> getTargetsForSite(Site site) {
        Set<Target> targets = new HashSet<>();
        for (Observation o : getObservations()) {
            if (o.getBlueprint().getSite() == site) {
                targets.add(o.getTarget());
            }
        }
        return targets;
    }

    /**
     * Gets all blueprints for observations at the given site.
     * @param site
     * @return
     */
    public Set<BlueprintBase> getBlueprintsForSite(Site site) {
        Set<BlueprintBase> blueprints = new HashSet<>();
        for (Observation o : getObservations()) {
            if (o.getBlueprint().getSite() == site) {
                blueprints.add(o.getBlueprint());
            }
        }
        return blueprints;
    }

    /**
     * Gets all conditions for observations at the given site.
     * @param site
     * @return
     */
    public Set<Condition> getConditionsForSite(Site site) {
        Set<Condition> conditions = new HashSet<>();
        for (Observation o : getObservations()) {
            if (o.getBlueprint().getSite() == site) {
                conditions.add(o.getCondition());
            }
        }
        return conditions;
    }

    public TacCategory getTacCategory() {
        return tacCategory;
    }

    public void setTacCategory(TacCategory tacCategory) {
        this.tacCategory = tacCategory;
    }

    public String getSubmissionsKey() {
        return submissionsKey;
    }

    public void setSubmissionsKey(String submissionsKey) {
        this.submissionsKey = submissionsKey;
    }

    public Itac getItac() {
        return itac;
    }

    public void setItac(Itac itac) {
        this.itac = itac;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public TimeAmount getTotalAwardedTime() {
        if (getItac() != null && getItac().getAccept() != null) {
            return getItac().getAccept().getAward();
        } else {
            return new TimeAmount(0, edu.gemini.tac.persistence.phase1.TimeUnit.HR);
        }
    }

    abstract public TimeAmount getTotalRequestedTime();
    abstract public TimeAmount getTotalMinRequestedTime();
    abstract public TimeAmount getTotalRecommendedTime();
    abstract public TimeAmount getTotalMinRecommendedTime();

    abstract public Submission getPrimary();
    public void setPrimary(final Submission s) {
        this.primarySubmission = s;
    }

    abstract public List<Submission> getSubmissions();
    abstract public void addSubmission(final Submission s);
    abstract public void clearSubmissions();

    /* Reach through the copy returned by getSubmissions to directly initialize the underlying
       elements (ngos and exchange) when appropriate.
     */
    abstract public void hibernateInitializeSubmissions();
    /**
     * This absolutely does not cover everything yet.  Right now, most of the elements are handled over
     * in the giant static method class HibernateToMutable.  This currently covers the multiple sub-class
     * specific conversions introduced by ProposalClass.
     * 
     * @return a partially converted mutable proposal
     * @param hibernateInvestigatorToIdMap
     */
    abstract public edu.gemini.model.p1.mutable.Proposal toMutable(final HashMap<Investigator, String> hibernateInvestigatorToIdMap);
    protected edu.gemini.model.p1.mutable.Proposal toMutable(edu.gemini.model.p1.mutable.Proposal mProposal, final HashMap<Investigator, String> hibernateInvestigatorToIdMap) {
        mProposal.setSchemaVersion(getSchemaVersion());
        mProposal.setTitle(getTitle());
        mProposal.setAbstract(getProposalAbstract());
        mProposal.setTacCategory(getTacCategory());
        mProposal.setScheduling(getScheduling());

        mProposal.setInvestigators(getInvestigators().toMutable(hibernateInvestigatorToIdMap));

        return mProposal;
    }

    public boolean isClassical() {
        return false;
    }
    public boolean isLargeProgram() {
        return false;
    }
    public boolean isToo() {
        return false;
    }
    public boolean isQueue() {
        return false;
    }
    public boolean isExchange() {
        return false;
    }

    /** TooOption: base class returns None; overridden as appropriate */
    public TooOption getTooOption(){
        return TooOption.NONE;
    }

    public String getObservingMode() {
        final String queueOrClassicalOrExchange = (isClassical()) ? "Classical" : (isExchange() ? "Exchange" : "Queue");
        final String tooAddition = (isToo()) ? ("+" + getTooOptionObservingModeClassString()) : "";

        return queueOrClassicalOrExchange + tooAddition;
    }

    public String getTooOptionObservingModeClassString(){
        switch(getTooOption()) {
            case NONE:
                return "";
            case RAPID:
                return "rapidToO";
            case STANDARD:
                return "standardToO";
            default:
                return "Did not account for new ToO enum (p1p(getTooOptionObservingModeClassString()))";
        }
    }

    public List<Observation> getBand3Observations() {
        return getActiveObservationsForBand(Band.BAND_3);
    }

    public List<Observation> getBand1Band2ActiveObservations(){
        return getActiveObservationsForBand(Band.BAND_1_2);
    }

    private List<Observation> getActiveObservationsForBand(Band b){
        final ArrayList<Observation> activeOs = new ArrayList<>();

        for(Observation o : observations) {
            if(o.getBand().equals(b) && o.getActive()){
                activeOs.add(o);
            }
        }
        return activeOs;
    }

    public List<Condition> getBand3Conditions() {
        final ArrayList<Condition> band3Conditions = new ArrayList<>();

        for (Observation o : observations) {
            if (o.getBand().equals(Band.BAND_3) && o.getActive())
                band3Conditions.add(o.getCondition());
        }

        return band3Conditions;
    }

    public String getBand3ConditionsDisplay() {
        final List<Observation> band3Observations = getBand3Observations();
        final List<String> conditions = new ArrayList<>();
        for (Observation o : band3Observations) {
            conditions.add(o.getCondition().getDisplay());
        }

        return StringUtils.join(conditions, ",");
    }

    public String getResourcesDisplay() {
        Set<String> resources = new HashSet<>();

        for (BlueprintBase b : blueprints) {
            resources.add(b.getDisplay());
        }

        return StringUtils.join(resources, ",");
    }

    public String getInstrumentsDisplay() {
        final Set<String> instruments = new TreeSet<>();
        for (BlueprintBase bb: getBlueprints()) {
            instruments.add(bb.getInstrument().getDisplayName());
        }

        return StringUtils.join(instruments, ",");
    }

    /**
     * Clones each member individually.  Relies on the entire graph being available at call-time.
     *
     * @return new proposal cloned from this one.
     */
    public abstract PhaseIProposal memberClone();

    /**
     * Best effort clone.  Happily swallows LazyInitializationExceptions and treats them as null assignements.  Use this
     * when trying to control how deep down the rabbit hole a transient instance peeks.
     *
     * @return a trimmed proposal clone
     */
    public abstract PhaseIProposal memberCloneLoaded();

    public static PhaseIProposal fromMutable(final edu.gemini.model.p1.mutable.Proposal mp, final List<Partner> partners) {
        final ProposalClassChoice choice = mp.getProposalClass();
        final ExchangeProposalClass exchangeProposalClass = choice.getExchange();
        final ClassicalProposalClass classicalProposalClass = choice.getClassical();
        final QueueProposalClass queueProposalClass = choice.getQueue();
        final SpecialProposalClass specialProposalClass = choice.getSpecial();
        final LargeProgramClass largeProgram = choice.getLarge();

        if (exchangeProposalClass != null) {
            return new ExchangeProposal(mp, exchangeProposalClass, partners);
        } else if (classicalProposalClass != null) {
            return new ClassicalProposal(mp, classicalProposalClass, partners);
        } else if (queueProposalClass != null) {
            return new QueueProposal(mp, queueProposalClass, partners);
        } else if (specialProposalClass != null) {
            return new SpecialProposal(mp, specialProposalClass, partners);
        } else if (largeProgram != null) {
            return new LargeProgram(mp, largeProgram, partners);
        } else {
            throw new RuntimeException("Unable to convert from mutable: " + mp);
        }
    }

    public Proposal getParent() {
        return parent;
    }

    public void setParent(final Proposal parent) {
        this.parent = parent;
    }

    public void normalizeInvestigator(final Investigator replaceMe, final Investigator with) {
        with.updateFrom(replaceMe);

        // -- principal investigator in investigator/pi
        if (getInvestigators().getPi().equals(replaceMe)) {
            getInvestigators().setPi((edu.gemini.tac.persistence.phase1.PrincipalInvestigator) with);
        }

        // -- co-investigators in investigators/coi
        if (getInvestigators().getCoi().contains(replaceMe)) {
            getInvestigators().getCoi().remove(replaceMe);
            getInvestigators().getCoi().add((edu.gemini.tac.persistence.phase1.CoInvestigator) with);
        }
    }

    public boolean isMixedObservationBands() {
        return !isUniformObservationBands();
    }

    public boolean isUniformObservationBands() {
        boolean containsBand12 = false;
        boolean containsBand3 = false;

        for (Observation o : getObservations()) {
            if (o.getBand().equals(Band.BAND_1_2))
                containsBand12 = true;
            else if (o.getBand().equals(Band.BAND_3))
                containsBand3 = true;
        }

        return containsBand12 ^ containsBand3;
    }

    public String getScheduling() {
        return scheduling;
    }

    public void setScheduling(String scheduling) {
        this.scheduling = scheduling;
    }
}
