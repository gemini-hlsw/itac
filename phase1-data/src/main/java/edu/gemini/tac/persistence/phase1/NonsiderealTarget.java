package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.NonSiderealTarget;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.util.Conversion;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.*;

@Entity
@DiscriminatorValue("NonsiderealTarget")
public class NonsiderealTarget extends Target {
    public static final int MAX_DISPLAY_ELEMENTS = 10;
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "target")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected List<EphemerisElement> ephemeris = new ArrayList<EphemerisElement>();

    public NonsiderealTarget() {}

    public NonsiderealTarget(final Target that, final PhaseIProposal phaseIProposal, final String targetId) {
        super(that, phaseIProposal, targetId);
        NonsiderealTarget nsThat = (NonsiderealTarget) that;
        for(EphemerisElement ee : nsThat.getEphemeris()){
            final EphemerisElement newElement = new EphemerisElement(ee, this);
            this.ephemeris.add(newElement);
        }
    }

    @Override
    public edu.gemini.model.p1.mutable.Target toMutable() {
        final NonSiderealTarget nonSiderealTarget = new NonSiderealTarget();
        nonSiderealTarget.setEpoch(getEpoch());
        nonSiderealTarget.setName(getName());
        nonSiderealTarget.setId(getTargetId());

        for (EphemerisElement ee : getEphemeris()) {
            edu.gemini.model.p1.mutable.EphemerisElement mee = new edu.gemini.model.p1.mutable.EphemerisElement();

            ee.getCoordinates().toMutable(mee);
            mee.setMagnitude(ee.getMagnitude());
            mee.setValidAt(Conversion.dateToXmlGregorian(ee.getValidAt()));

            nonSiderealTarget.getEphemeris().add(mee);
        }

        return nonSiderealTarget;
    }

    @Override
    public Target copy() {
        final NonsiderealTarget nonsiderealTarget = new NonsiderealTarget(this, phaseIProposal, targetId);
        for (EphemerisElement e : getEphemeris()) {
            nonsiderealTarget.getEphemeris().add(new EphemerisElement(e, nonsiderealTarget));
        }
        nonsiderealTarget.setEpoch(getEpoch());
        nonsiderealTarget.setName(getName());
        nonsiderealTarget.setPhaseIProposal(getPhaseIProposal());
        if(getTargetId() != null){
            nonsiderealTarget.setTargetId(getTargetId());
        }else{
            nonsiderealTarget.setTargetId("");
        }
        return nonsiderealTarget;
    }

    @Override
    public int getRaBin() {
        // use first ephemeris coordinate as the coordinate for the bin for non-sidereal targets
        Coordinates c = getEphemeris().get(0).getCoordinates();
        return (int)(c.getRa().toHours().getMagnitude()) + 1;
    }

    @Override
    public String getDisplayCoordinates() {
        return getEphemeris().get(0).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NonsiderealTarget)) return false;
        if (!super.equals(o)) return false;

        NonsiderealTarget that = (NonsiderealTarget) o;

        if (ephemeris != null ? !ephemeris.equals(that.ephemeris) : that.ephemeris != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (ephemeris != null ? ephemeris.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NonsiderealTarget{" +
                "id=" + id +
                ", ephemeris=" + ephemeris +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toKml() {
        throw new NotImplementedException();
    }

    public List<EphemerisElement> getEphemeris() {
        return ephemeris;
    }

    public void setEphemeris(List<EphemerisElement> ephemeris) {
        this.ephemeris = ephemeris;
    }

    public Coordinates findOrNull(Date d){
        Collections.sort(ephemeris, new Comparator<EphemerisElement>() {
            public int compare(EphemerisElement a, EphemerisElement b){ return a.getValidAt().compareTo(b.getValidAt()); }
            public boolean equals(Object o){ return false; }
        });

        Coordinates coordinatesOrNull = findOrNull(ephemeris, d, 0, 1);
        
        if (coordinatesOrNull == null) {// Unable to to find ephemeris that cover the date d (usage often the midpoint of the semester).
            final long targetTime = d.getTime();
            long minimumTimeDifference = Long.MAX_VALUE;

            for (EphemerisElement ee : ephemeris) {
                final Date validAt = ee.getValidAt();
                final long timeDifference = Math.abs(validAt.getTime() - targetTime);
                if (timeDifference < minimumTimeDifference)
                    coordinatesOrNull = ee.getCoordinates();
            }
        }

        return coordinatesOrNull;
    }

    private Coordinates findOrNull(List<EphemerisElement> ees, Date d, int head, int tail){
        if(ees.size() == tail){
            if(ees.size() == 1){
                //Special case -- single ephemeris
                return ees.get(0).getCoordinates();
            }
            //End of list
            return null;
        }
        final EphemerisElement a = ees.get(head);
        final EphemerisElement b = ees.get(tail);
        if(a.validAt.before(d) && b.validAt.after(d)){
            return a.interpolate(d, b);
        }
        return findOrNull(ees, d, tail, tail + 1);
    }

    @Override
    public String toDot(){
        String myDot = "NonsiderealTarget_" + (id == null ? "NULL" : id.toString());
        String dot = myDot + ";\n";
        if(ephemeris == null || ephemeris.size() == 0){
            dot += myDot + "->EMPTY; EMPTY;\n";
        } else {
            for(EphemerisElement ee : ephemeris){
                dot += myDot + "->" + ee.toDot() + ";\n";
            }
        }
        
        return dot;
    }

    @Override
    public String getPositionDisplay() {
        final List<EphemerisElement> ephemerisElements = (getEphemeris().size() > 10) ? getEphemeris().subList(0, MAX_DISPLAY_ELEMENTS) : getEphemeris();
        final List<String> display = new ArrayList<String>();
        for (EphemerisElement ee : ephemerisElements) {
            display.add(ee.getDisplay());
        }
        if (getEphemeris().size() > MAX_DISPLAY_ELEMENTS)
            display.add("...");

        return StringUtils.join(display,",");
    }

    @Override
    public String getBrightnessDisplay() {
        final List<EphemerisElement> ephemerisElements = (getEphemeris().size() > 10) ? getEphemeris().subList(0, MAX_DISPLAY_ELEMENTS) : getEphemeris();
        final List<String> display = new ArrayList<String>();
        for (EphemerisElement ee : ephemerisElements) {
            if (ee.getMagnitude() != null)
                display.add(String.format("%.3f",ee.getMagnitude()));

        }
        if (getEphemeris().size() > MAX_DISPLAY_ELEMENTS)
            display.add("...");

        return StringUtils.join(display,",");
    }

    @Override
    public void accept(TargetVisitor visitor) { visitor.visit(this); }

    public String getTargetClass() { return "Nonsidereal";};
    public boolean isNonSidereal() { return true; }

}
