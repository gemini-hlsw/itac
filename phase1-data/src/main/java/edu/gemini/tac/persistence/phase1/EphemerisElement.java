package edu.gemini.tac.persistence.phase1;

import edu.gemini.shared.skycalc.Angle;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "v2_ephemeris_elements")
public class EphemerisElement {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "magnitude")
    protected BigDecimal magnitude;

    @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "coordinate_id")
    @Cascade( { org.hibernate.annotations.CascadeType.SAVE_UPDATE, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    protected Coordinates coordinates;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Target target;

    @Column(name = "valid_at")
    protected Date validAt;

    public EphemerisElement() {}
    public EphemerisElement(final EphemerisElement e, final NonsiderealTarget nonsiderealTarget) {
        this.setMagnitude(e.getMagnitude());
        this.setValidAt(e.getValidAt());
        final Coordinates coordinates1 = e.getCoordinates().copyAndTarget();
        this.setCoordinates(coordinates1);
        this.setTarget(nonsiderealTarget);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EphemerisElement)) return false;

        EphemerisElement that = (EphemerisElement) o;

        if (coordinates != null ? !coordinates.equals(that.coordinates) : that.coordinates != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (magnitude != null ? !magnitude.equals(that.magnitude) : that.magnitude != null) return false;
        if (validAt != null ? !validAt.equals(that.validAt) : that.validAt != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (magnitude != null ? magnitude.hashCode() : 0);
        result = 31 * result + (coordinates != null ? coordinates.hashCode() : 0);
        result = 31 * result + (validAt != null ? validAt.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EphemerisElement{" +
                "id=" + id +
                ", magnitude=" + magnitude +
                ", coordinates=" + coordinates +
                ", validAt=" + validAt +
                '}';
    }

    public String getDisplay() {
        return "[" +
                "magnitude=" + magnitude +
                ", coordinates=" + coordinates.toDisplayString() +
                ", validAt=" + validAt +
                "]";

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(BigDecimal magnitude) {
        this.magnitude = magnitude;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public Date getValidAt() {
        return validAt;
    }

    public void setValidAt(Date validAt) {
        this.validAt = validAt;
    }

    private double normalize(double angleInDegrees){
        if(angleInDegrees > 180.0){
            return normalize(angleInDegrees - 360.0);
        }
        if(angleInDegrees < -180.0){
            return normalize(angleInDegrees + 360.0);
        }
        return angleInDegrees;
    }

    private Angle delta(Angle a, Angle b){
        double aMagNormalized = normalize(a.getMagnitude());
        double bMagNormalized = normalize(b.getMagnitude());
        double delta = bMagNormalized - aMagNormalized;
        double normalizedDelta = normalize(delta);
        return new Angle(normalizedDelta, Angle.Unit.DEGREES);
    }

    public Coordinates interpolate(Date d, EphemerisElement that){
        Validate.isTrue(that.getValidAt().after(this.getValidAt()));
        Validate.isTrue(d.after(this.getValidAt()));

        final double percent = (d.getTime() - this.getValidAt().getTime()) / (double) (that.getValidAt().getTime() - this.getValidAt().getTime());
        final Angle thisRa = this.getCoordinates().getRa().convertTo(Angle.Unit.DEGREES);
        final Angle thatRa = that.getCoordinates().getRa().convertTo(Angle.Unit.DEGREES);
        final Angle thisDec = this.getCoordinates().getDec().convertTo(Angle.Unit.DEGREES);
        final Angle thatDec = that.getCoordinates().getDec().convertTo(Angle.Unit.DEGREES);

        Angle deltaRa = delta(thisRa, thatRa);

        final double deltaRaMag = deltaRa.getMagnitude() * percent;
        final Angle interpolatedRa = new Angle(thisRa.getMagnitude() + deltaRaMag, deltaRa.getUnit());

        final Angle deltaDec = delta(thisDec, thatDec);
        final double deltaDecMag = deltaDec.getMagnitude() * percent;
        final Angle interpolatedDec = new Angle(thisDec.getMagnitude() + deltaDecMag, deltaDec.getUnit());

        return new DegDegCoordinates(interpolatedRa, interpolatedDec);
    }

    public String toDot(){
        String myDot = "EphemerisElement_" + (id == null ? "NULL" : id.toString());
        String dot = myDot + ";\n";
        if(coordinates != null){
            dot += myDot + "->" + coordinates.toDot() + ";\n";
        }
        dot += myDot + "->" + "NonsiderealTarget_" + (target.getId() == null ? "NULL" : target.getId());
        return dot;
    }
}
