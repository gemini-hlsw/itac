package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.Magnitudes;
import edu.gemini.shared.skycalc.Angle;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cascade;
import org.w3c.dom.Element;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@DiscriminatorValue("SiderealTarget")
public class SiderealTarget extends Target {
    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "coordinate_id")
    @Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE})
    protected Coordinates coordinates;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, optional = true)
    @JoinColumn(name = "proper_motion_id")
    @Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE})
    protected ProperMotion properMotion;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "target")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Set<Magnitude> magnitudes = new HashSet<Magnitude>();

    public SiderealTarget() {
    }

    public SiderealTarget(final Target that, final PhaseIProposal phaseIProposal, final String targetId) {
        super(that, phaseIProposal, targetId);
        SiderealTarget stThat = (SiderealTarget) that;
        this.coordinates = stThat.getCoordinates();
        this.properMotion = stThat.getProperMotion();
        for (Magnitude thatMagnitude : stThat.getMagnitudes()) {
            Magnitude mClone = new Magnitude(thatMagnitude, this);
            magnitudes.add(mClone);
        }
    }

    /**
     * a la Rollover Web API
     * <target>
     * <ra>269.80833333333334</ra>
     * <dec>-29.32916666666665</dec>
     * </target>
     *
     * @param target
     */
    public SiderealTarget(Element target) {
        String raText = target.getElementsByTagName("ra").item(0).getTextContent();
        Angle ra = new Angle(Double.parseDouble(raText.replace(" deg", "")), Angle.Unit.DEGREES);
        String decText = target.getElementsByTagName("dec").item(0).getTextContent();
        Angle dec = new Angle(Double.parseDouble(decText.replace(" deg", "")), Angle.Unit.DEGREES);
        this.setCoordinates(new DegDegCoordinates(ra, dec));
        this.setName("");
    }

    @Override
    public edu.gemini.model.p1.mutable.Target toMutable() {
        final edu.gemini.model.p1.mutable.SiderealTarget mt = new edu.gemini.model.p1.mutable.SiderealTarget();

        mt.setEpoch(getEpoch());
        if (getProperMotion() != null)
            mt.setProperMotion(getProperMotion().toMutable());
        mt.setId(getTargetId());
        mt.setName(getName());
        if (magnitudes.size() > 0)
            mt.setMagnitudes(new Magnitudes());
        for (Magnitude m : magnitudes) {
            mt.getMagnitudes().getMagnitude().add(m.toMutable());
        }

        coordinates.toMutable(mt);

        return mt;
    }

    @Override
    public Target copy() {
        final SiderealTarget siderealTarget = new SiderealTarget(this, phaseIProposal, targetId);

        siderealTarget.setProperMotion(getProperMotion());
        siderealTarget.setCoordinates(coordinates.copyAndTarget());
        siderealTarget.setEpoch(getEpoch());
        siderealTarget.setName(getName());
        siderealTarget.setPhaseIProposal(getPhaseIProposal());
        if (getTargetId() != null) {
            siderealTarget.setTargetId(getTargetId());
        } else {
            siderealTarget.setTargetId("");
        }

        for (Magnitude m : getMagnitudes()) {
            siderealTarget.getMagnitudes().add(new Magnitude(m, siderealTarget));
        }

        return siderealTarget;
    }

    @Override
    public int getRaBin() {
        Coordinates c = getCoordinates();
        return (int)(c.getRa().toHours().getMagnitude()) + 1;
    }

    @Override
    public String getDisplayCoordinates() {
        return getCoordinates().toDisplayString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SiderealTarget)) return false;
        if (!super.equals(o)) return false;

        SiderealTarget that = (SiderealTarget) o;

        if (coordinates != null ? !coordinates.equals(that.coordinates) : that.coordinates != null) return false;
        if (magnitudes != null ? !magnitudes.equals(that.magnitudes) : that.magnitudes != null) return false;
        if (properMotion != null ? !properMotion.equals(that.properMotion) : that.properMotion != null) return false;

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
        if (!(o instanceof SiderealTarget)) return false;
        if (!super.equalsForComponent(o)) return false;

        SiderealTarget that = (SiderealTarget) o;

        if (coordinates != null ? !coordinates.equals(that.coordinates) : that.coordinates != null) return false;
        if (magnitudes != null ? !magnitudes.equals(that.magnitudes) : that.magnitudes != null) return false;
        if (properMotion != null ? !properMotion.equals(that.properMotion) : that.properMotion != null) return false;

        return true;
    }

        @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (coordinates != null ? coordinates.hashCode() : 0);
        result = 31 * result + (properMotion != null ? properMotion.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SiderealTarget{" +
                "coordinates=" + coordinates +
                ", properMotion=" + properMotion +
                ", magnitudes=" + magnitudes +
                '}';
    }

    @Override
    public String toKml() {
        return "<LookAt>\n" +
                "      <longitude>" + getCoordinates().toKmlLongitude() + "</longitude>\n" +
                "      <latitude>" + getCoordinates().toKmlLatitude() + "</latitude>\n" +
                "      <altitude>0</altitude>\n" +
                "      <range>10000</range>\n" +
                "      <tilt>0</tilt>\n" +
                "      <heading>0</heading>\n" +
                "    </LookAt>\n" +
                "    <styleUrl>#ScienceTarget</styleUrl>\n" +
                "    <Point>\n" +
                getCoordinates().toKml() +
                "    </Point>";

    }

    public Set<Magnitude> getMagnitudes() {
        return magnitudes;
    }

    public void setMagnitudes(Set<Magnitude> magnitudes) {
        this.magnitudes = magnitudes;
    }

    public ProperMotion getProperMotion() {
        return properMotion;
    }

    public void setProperMotion(ProperMotion properMotion) {
        this.properMotion = properMotion;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public String toDot() {
        String myDot = "SiderealTarget_" + (id == null ? "NULL" : id.toString()) + ";\n";
        return myDot;
    }

    @Override
    public String getPositionDisplay() {
        return ((coordinates != null) ? coordinates.toDisplayString() : "No coordinates available.") + " " + ((properMotion != null) ? properMotion.toString() : "");
    }

    @Override
    public String getBrightnessDisplay() {
        if (magnitudes == null)
            return "";

        final List<String> display = new ArrayList<String>(magnitudes.size());
        for (Magnitude m : magnitudes)
            display.add(m.toDisplayString());

        return StringUtils.join(display, ",");
    }

    @Override
    public void accept(TargetVisitor visitor) {
        visitor.visit(this);
    }

    public String getTargetClass() { return "Sidereal";};
    public boolean isSidereal() { return true; }
}
