package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.Band;
import edu.gemini.tac.persistence.IValidateable;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@NamedQueries(
        @NamedQuery(
        name = "observation.observationsForEditingById",
                query = "from Observation o " +
                        "join fetch o.target t " +
                        "join fetch o.condition " +
                        "where o.id in (:observationIds)"
        )
)
@Entity
@Table(name = "v2_observations")
public class Observation implements IValidateable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    @org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Target target;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id")
    @org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Condition condition;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id")
    @org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected BlueprintBase blueprint;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value",
                    column=@Column(name = "prog_time_amount_value")),
            @AttributeOverride(name = "units",
                    column=@Column(name = "prog_time_amount_unit"))
    })
    protected TimeAmount progTime;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value",
                    column=@Column(name = "part_time_amount_value")),
            @AttributeOverride(name = "units",
                    column=@Column(name = "part_time_amount_unit"))
    })
    protected TimeAmount partTime;

    @Embedded
    protected TimeAmount time;

    @OneToMany(cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, mappedBy = "observation")
    @org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Set<GuideStar> guideStars = new HashSet<GuideStar>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_i_proposal_id")
    protected PhaseIProposal proposal;

    @Enumerated(EnumType.STRING)
    @Column(name = "band", nullable = false)
    protected Band band;

    @Embedded
    protected ObservationMetaData metaData;

    @Column(name = "active")
    protected Boolean active = Boolean.TRUE;

    /**
     * Copy constructor.
     *
     * @param copied observation to copy.
     * @param phaseIProposal phaseIProposal that the new observation will belong to.
     */
    public Observation(final Observation copied, final PhaseIProposal phaseIProposal) {
        /*
        TODO:
        This is called during JointProposal.duplicate(). Open question: if the alreadyHad*
        booleans are false, should the Observation be pushed into the phaseIProposal source?
        I don't think so, but needs review / walkthrough
         */

        final Target copiedTarget = copied.getTarget();
        final Condition copiedCondition = copied.getCondition();
        final BlueprintBase copiedBlueprint = copied.getBlueprint();
        final Set<GuideStar> copiedGuidestars = copied.getGuideStars();

        boolean alreadyHadTarget = false;
        for (Target t: phaseIProposal.getTargets()) {
            if (t.getTargetId().equals(copiedTarget.getTargetId())) {
                setTarget(t);
                alreadyHadTarget = true;
            }
        }
        if(! alreadyHadTarget){
            setTarget(copiedTarget);
        }

        boolean alreadyHadCondition = false;
        for (Condition c: phaseIProposal.getConditions()) {
            if (c.getConditionId().equals(copiedCondition.getConditionId())) {
                setCondition(c);
                alreadyHadCondition = true;
            }
        }
        if (!alreadyHadCondition) {
            setCondition(copiedCondition);
        }

        boolean alreadyHadBlueprint = false;
        for (BlueprintBase b : phaseIProposal.getBlueprints()) {
            if (b.getBlueprintId().equals(copiedBlueprint.getBlueprintId())) {
                setBlueprint(b);
                alreadyHadBlueprint = true;
            }
        }
        if(! alreadyHadBlueprint){
            setBlueprint(copiedBlueprint);
        }

        for (GuideStar g : copiedGuidestars) {
            guideStars.add(new GuideStar(g, this));
        }

        setTime(new TimeAmount(copied.getTime()));
        setProposal(phaseIProposal);
        setBand(copied.getBand());
        if (copied.getMetaData() != null) {
            setMetaData(new ObservationMetaData(copied.getMetaData()));
        }

        validate();
    }

    public Observation() {}

    public Target getTarget() {
        return target;
    }

    public Condition getCondition() {
        return condition;
    }

    public BlueprintBase getBlueprint() {
        return blueprint;
    }

    public TimeAmount getProgTime() { return progTime; }

    public TimeAmount getPartTime() { return partTime; }

    public TimeAmount getTime() {
        return time;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public void setBlueprint(BlueprintBase blueprint) {
        this.blueprint = blueprint;
    }

    public void setProgTime(TimeAmount progTime) { this.progTime = progTime; }

    public void setPartTime(TimeAmount partTime) { this.partTime = partTime; }

    public void setTime(TimeAmount time) {
        this.time = time;
    }

    public void setGuideStars(Set<GuideStar> guideStars) {
        this.guideStars = guideStars;
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<GuideStar> getGuideStars() {
        return guideStars;

    }

    public PhaseIProposal getProposal() {
        return proposal;
    }

    public void setProposal(PhaseIProposal proposal) {
        this.proposal = proposal;
    }

    public void validate() {
        if ((target == null) || (blueprint == null) || condition == null)
            throw new IllegalStateException("Observations must have a blueprint, a condition and a target.");
    }

    public Band getBand() {
        return band;
    }

    public void setBand(Band band) {
        this.band = band;
    }

    @Override
    public String toString() {
        return "Observation{" +
                "id=" + id +
                ", target=" + target +
                ", condition=" + condition +
                ", blueprint=" + blueprint +
                ", time=" + time +
                ", guideStars=" + guideStars +
                ", proposal.title =" + proposal.getTitle() +
                ", band=" + band +
                ", metaData=" + metaData +
                ", active=" + active +
                '}';
    }

    public String toDot(){
        String myDot = "Observation_"  +  ((id == null) ? "NULL" : id.toString());
        String dot = myDot + ";\n";
        dot += myDot + "->" + target.toDot() + ";\n";

        return dot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Observation)) return false;

        Observation that = (Observation) o;

        if (band != that.band) return false;
        if (blueprint != null ? !blueprint.equals(that.blueprint) : that.blueprint != null) return false;
        if (condition != null ? !condition.equals(that.condition) : that.condition != null) return false;
        if (guideStars != null ? !guideStars.equals(that.guideStars) : that.guideStars != null) return false;
        if (metaData != null ? !metaData.equals(that.metaData) : that.metaData != null) return false;
        if (proposal != null ? !proposal.equals(that.proposal) : that.proposal != null) return false;
        if (target != null ? !target.equals(that.target) : that.target != null) return false;
        if (time != null ? !time.equals(that.time) : that.time != null) return false;

        return true;
    }

    /**
     * Looser version of equals in order to support joint->component observation identification for
     * propagation of edits.  Needs equivalent band, blueprint, condition, target and time.
     *
     * @param o
     * @return
     */
    public boolean equalsForComponent(Object o) {
        if (this == o) return true;
        if (!(o instanceof Observation)) return false;

        Observation that = (Observation) o;

        if (band != that.band) return false;
        if (blueprint != null ? !blueprint.equalsForComponent(that.blueprint) : that.blueprint != null) return false;
        if (condition != null ? !condition.equals(that.condition) : that.condition != null) return false;
        if (target != null ? !target.equals(that.target) : that.target != null) return false;
        if (time != null ? !time.equals(that.time) : that.time != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = target != null ? target.hashCode() : 0;
        result = 31 * result + (condition != null ? condition.hashCode() : 0);
        result = 31 * result + (blueprint != null ? blueprint.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (proposal != null ? proposal.hashCode() : 0);
        result = 31 * result + (band != null ? band.hashCode() : 0);
        result = 31 * result + (metaData != null ? metaData.hashCode() : 0);
        return result;
    }

    public ObservationMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(ObservationMetaData metaData) {
        this.metaData = metaData;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
