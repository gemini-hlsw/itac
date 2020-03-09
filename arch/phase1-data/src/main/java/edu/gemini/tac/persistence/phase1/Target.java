package edu.gemini.tac.persistence.phase1;


import edu.gemini.model.p1.mutable.CoordinatesEpoch;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Random;

@NamedQueries({
        @NamedQuery(name = "target.siderealTargetsForEditingByEntityList",
            query = "from SiderealTarget t " +
                    "join fetch t.coordinates " +
                    "left join fetch t.properMotion " +
                    "left join fetch t.magnitudes " +
                    "where t in (:targets)"
        ),
        @NamedQuery(name = "target.nonsiderealTargetsForEditingByEntityList",
            query = "from NonsiderealTarget t " +
                    "left join fetch t.ephemeris ee " +
                    "left join fetch ee.coordinates " +
                    "where t in (:targets)"
        ),
        @NamedQuery(name = "target.tooTargetsForEditingByEntityList",
            query = "from TooTarget t " +
                    "where t in (:targets)")
})
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Table(name = "v2_targets")
public abstract class Target implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Column(name = "name", nullable = false)
    protected String name;

    @Column(name = "target_id")
    protected String targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_i_proposal_id")
    protected PhaseIProposal phaseIProposal;

    @Enumerated(EnumType.STRING)
    @Column(name = "epoch", nullable = false)
    protected CoordinatesEpoch epoch = CoordinatesEpoch.J_2000;

    @Transient
    static final Random random = new Random();

    public Target() {}

    protected Target(final Target that, final PhaseIProposal phaseIProposal, final String targetId) {
        this.setName(that.getName());
        this.setTargetId(targetId);
        this.setPhaseIProposal(phaseIProposal);
    }

    public abstract edu.gemini.model.p1.mutable.Target toMutable();

    /**
     * Copies everything except the phase I proposal.  Intended to be useful
     * when duplicating a phaseIProposal wholly.
     */
    public abstract Target copy();

    /**
     * Gets the bin this target fall into (used for reporting).
     * @return
     */
    public abstract int getRaBin();

    /**
     * Gets a textual representation of the coordinates of this target (used for reporting).
     * @return
     */
    public abstract String getDisplayCoordinates();

    protected Target(final Target that, final PhaseIProposal phaseIProposal) {
        this(that, phaseIProposal, that.getTargetId());
    }

    /**
     * Copies everything about target except for the target id, which it
     * will generate randomly.  Intended to create an additional copy within
     * a proposal.
     *
     * @param that The target that will serve as a prototype.
     */
    public Target(final Target that) {
        this(that, that.getPhaseIProposal(), "target-" + random.nextLong());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Target)) return false;

        Target target = (Target) o;

        if (epoch != target.epoch) return false;
        if (name != null ? !name.equals(target.name) : target.name != null) return false;
        if (phaseIProposal != null ? !phaseIProposal.equals(target.phaseIProposal) : target.phaseIProposal != null)
            return false;
        if (targetId != null ? !targetId.equals(target.targetId) : target.targetId != null) return false;

        return true;
    }

    /**
     * Looser version of equals in order to support joint->component observation identification for
     * propagation of edits.  Needs equivalent epoch and name.
     *
     * @param o
     * @return
     */
    public boolean equalsForComponent(Object o) {
        if (this == o) return true;
        if (!(o instanceof Target)) return false;

        Target target = (Target) o;

        if (epoch != target.epoch) return false;
        if (name != null ? !name.equals(target.name) : target.name != null) return false;

        return true;
    }

        @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (targetId != null ? targetId.hashCode() : 0);
        result = 31 * result + (phaseIProposal != null ? phaseIProposal.hashCode() : 0);
        result = 31 * result + (epoch != null ? epoch.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Target{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", targetId='" + targetId + '\'' +
                ", epoch=" + epoch +
                '}';
    }

    public CoordinatesEpoch getEpoch() {
        return epoch;
    }

    public void setEpoch(CoordinatesEpoch epoch) {
        this.epoch = epoch;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public PhaseIProposal getPhaseIProposal() {
        return phaseIProposal;
    }

    public void setPhaseIProposal(PhaseIProposal phaseIProposal) {
        this.phaseIProposal = phaseIProposal;
    }

    public abstract String toKml();

    public abstract String toDot();

    public abstract String getPositionDisplay();

    public abstract String getBrightnessDisplay();

    //Visitor pattern
    public abstract void accept(TargetVisitor visitor); //Always implemented as visitor.visit(this);

    public abstract String getTargetClass();
    public boolean isToo() { return false; }
    public boolean isSidereal() { return false; }
    public boolean isNonSidereal() { return false; }
}
