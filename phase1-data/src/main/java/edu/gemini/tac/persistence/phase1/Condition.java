package edu.gemini.tac.persistence.phase1;

import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.sitequality.CloudCover;
import edu.gemini.tac.persistence.phase1.sitequality.ImageQuality;
import edu.gemini.tac.persistence.phase1.sitequality.SkyBackground;
import edu.gemini.tac.persistence.phase1.sitequality.WaterVapor;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Entity
@Table(name = "v2_conditions")
public class Condition {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "name")
    protected String name;

    @Column(name = "max_airmass", nullable = true)
    protected BigDecimal maxAirmass;

    @Enumerated(EnumType.STRING)
    @Column(name = "cloud_cover", nullable = false)
    protected CloudCover cloudCover;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_quality", nullable = false)
    protected ImageQuality imageQuality;

    @Enumerated(EnumType.STRING)
    @Column(name = "sky_background", nullable = false)
    protected SkyBackground skyBackground;

    @Enumerated(EnumType.STRING)
    @Column(name = "water_vapor", nullable = false)
    protected WaterVapor waterVapor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_i_proposal_id", nullable = true)
    protected PhaseIProposal phaseIProposal;

    /**
     * This property really only has meaning for XML serialization and is
     * strictly required to be unique to allow the XML to be unambiguous.
     */
    @Column(name = "condition_id")
    protected String conditionId;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "condition")
    protected Set<Observation> observations = new HashSet<Observation>();

    @Transient
    static final Random random = new Random();

    public Condition(final Condition that, final PhaseIProposal phaseIProposal, final String conditionId) {
        this.setPhaseIProposal(phaseIProposal);
        this.setConditionId(conditionId);

        this.setName(that.getName());
        this.setCloudCover(that.getCloudCover());
        this.setMaxAirmass(that.getMaxAirmass());
        this.setImageQuality(that.getImageQuality());
        this.setSkyBackground(that.getSkyBackground());
        this.setWaterVapor(that.getWaterVapor());
    }

    /**
     * Copies everything about a condition except for the condition id, which it
     * will generate randomly.  Intended to create an additional copy within
     * a proposal.
     *
     * @param that The target that will serve as a prototype.
     */
    public Condition(final Condition that) {
        this(that, that.getPhaseIProposal(), that.getConditionId());
    }

    public Condition(edu.gemini.model.p1.mutable.Condition that){
        setConditionId(that.getId());
        setName(that.getName());
        setCloudCover(CloudCover.fromValue(that.getCc().value()));
        setMaxAirmass(that.getMaxAirmass());
        setImageQuality(ImageQuality.fromValue(that.getIq().value()));
        setSkyBackground(SkyBackground.fromValue(that.getSb().value()));
        setWaterVapor(WaterVapor.fromValue(that.getWv().value()));
    }

    /**
     * Copies everything except the phase I proposal.  Intended to be useful
     * when duplicating a phaseIProposal wholly.
     *
     * @param that - condition to be copied.
     * @param phaseIProposal - phaseIProposal that this condition belongs to.
     */
    public Condition(final Condition that, final PhaseIProposal phaseIProposal) {
        this(that, phaseIProposal, that.getConditionId());
    }

    /** Hibernate and testing */
    public Condition() {}

    /**
     * As defined by Rollover Web API:
     * <conditions>
     *   <cc>70</cc>
     *   <iq>70</iq>
     *   <sb>100</sb>
     *   <wv>100</wv>
     * </conditions>
     * @param conditions
     */
    public Condition(Element conditions) {
        String ccText = conditions.getElementsByTagName("cc").item(0).getTextContent();
        this.setCloudCover(CloudCover.fromValue(Byte.parseByte(ccText)));
        String iqText = conditions.getElementsByTagName("iq").item(0).getTextContent();
        this.setImageQuality(ImageQuality.fromValue(Byte.parseByte(iqText)));
        String sbText = conditions.getElementsByTagName("sb").item(0).getTextContent();
        this.setSkyBackground(SkyBackground.fromValue(Byte.parseByte(sbText)));
        String wvText = conditions.getElementsByTagName("wv").item(0).getTextContent();
        this.setWaterVapor(WaterVapor.fromValue(Byte.parseByte(wvText)));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Condition)) return false;

        Condition condition = (Condition) o;

        if (cloudCover != condition.cloudCover) return false;
        if (imageQuality != condition.imageQuality) return false;
        if (maxAirmass != null ? !maxAirmass.equals(condition.maxAirmass) : condition.maxAirmass != null) return false;
        if (name != null ? !name.equals(condition.name) : condition.name != null) return false;
        if (skyBackground != condition.skyBackground) return false;
        if (waterVapor != condition.waterVapor) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (maxAirmass != null ? maxAirmass.hashCode() : 0);
        result = 31 * result + (cloudCover != null ? cloudCover.hashCode() : 0);
        result = 31 * result + (imageQuality != null ? imageQuality.hashCode() : 0);
        result = 31 * result + (skyBackground != null ? skyBackground.hashCode() : 0);
        result = 31 * result + (waterVapor != null ? waterVapor.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Condition{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", maxAirmass=" + maxAirmass +
                ", cloudCover=" + cloudCover +
                ", imageQuality=" + imageQuality +
                ", skyBackground=" + skyBackground +
                ", waterVapor=" + waterVapor +
                ", conditionId='" + conditionId + '\'' +
                '}';
    }

    public String toDot(){
        String myDot = "Condition_"  +  ((id == null) ? "NULL" : id.toString()) + ";\n";
        return myDot;
    }

    public String getName() {
        if (name != null)
            return name;
        else {
            return joinConditionDisplayNames();
        }
    }

    public String getDisplay() {
        if (name != null) {
            return name + "[" + joinConditionDisplayNames() + "]";
        }
        else {
            return joinConditionDisplayNames();
        }
    }

    public String getActiveDisplay() {
        return joinConditionDisplayNames();
    }

    private String joinConditionDisplayNames() {
        final String[] conditionDisplayNames = {
                "IQ" + imageQuality.getDisplayName(),
                "SB" + skyBackground.getDisplayName(),
                "WV" +waterVapor.getDisplayName(),
                "CC" + cloudCover.getDisplayName(),
        };

        return StringUtils.join(conditionDisplayNames, ",");
    }

    /**
     * Defines a partial order on conditions.
     * A condition is better (or relaxed) than another one if any of its parts is better
     * (i.e. the percentage values are lower).
     * @param other condition to compare to...
     * @return true if any of this instance's percentage values are lower than the other
     */
    public boolean isBetterThan(final Condition other) {
        return
                (this.getCloudCover().getPercentage() < other.getCloudCover().getPercentage() ||
                        this.getImageQuality().getPercentage() < other.getImageQuality().getPercentage() ||
                        this.getSkyBackground().getPercentage() < other.getSkyBackground().getPercentage() ||
                        this.getWaterVapor().getPercentage() < other.getWaterVapor().getPercentage());
    }

    public CloudCover getCloudCover() {
        return cloudCover;
    }

    public ImageQuality getImageQuality() {
        return imageQuality;
    }

    public SkyBackground getSkyBackground() {
        return skyBackground;
    }

    public WaterVapor getWaterVapor() {
        return waterVapor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PhaseIProposal getPhaseIProposal() {
        return phaseIProposal;
    }

    public void setPhaseIProposal(PhaseIProposal phaseIProposal) {
        this.phaseIProposal = phaseIProposal;
    }

    public String getConditionId() {
        return conditionId;
    }

    public void setConditionId(String conditionId) {
        this.conditionId = conditionId;
    }

    public BigDecimal getMaxAirmass() {
        return maxAirmass;
    }

    public void setMaxAirmass(BigDecimal maxAirmass) {
        this.maxAirmass = maxAirmass;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCloudCover(CloudCover cloudCover) {
        this.cloudCover = cloudCover;
    }

    public void setImageQuality(ImageQuality imageQuality) {
        this.imageQuality = imageQuality;
    }

    public void setSkyBackground(SkyBackground skyBackground) {
        this.skyBackground = skyBackground;
    }

    public void setWaterVapor(WaterVapor waterVapor) {
        this.waterVapor = waterVapor;
    }

    public Set<Observation> getObservations() {
        return observations;
    }

    public edu.gemini.model.p1.mutable.Condition toPhase1(){
        edu.gemini.model.p1.mutable.Condition c = new edu.gemini.model.p1.mutable.Condition();
        c.setCc(edu.gemini.model.p1.mutable.CloudCover.fromValue(this.getCloudCover().value()));
        c.setIq(edu.gemini.model.p1.mutable.ImageQuality.fromValue(this.getImageQuality().value()));
        c.setSb(edu.gemini.model.p1.mutable.SkyBackground.fromValue(this.getSkyBackground().value()));
        c.setWv(edu.gemini.model.p1.mutable.WaterVapor.fromValue(this.getWaterVapor().value()));
        return c;
    }

    public Enum[] getConditionOrdering() {
        return new Enum[] { imageQuality, skyBackground, waterVapor, cloudCover };
    }
}
