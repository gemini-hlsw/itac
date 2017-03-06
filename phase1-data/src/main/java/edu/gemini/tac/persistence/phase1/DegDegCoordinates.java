package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.SiderealTarget;
import edu.gemini.shared.skycalc.Angle;
import edu.gemini.shared.skycalc.SkycalcArgumentException;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("DegDeg")
@BatchSize(size = 500)
public class DegDegCoordinates extends Coordinates {
    @Column(name = "deg_ra")
    protected BigDecimal ra;

    @Column(name = "deg_dec")
    protected BigDecimal dec;

    public DegDegCoordinates() {
    }

    public DegDegCoordinates(Coordinates that) {
        this.setDec(that.getDec());
        this.setRa(that.getRa());
    }

    public DegDegCoordinates(Angle ra, Angle dec) {
        this.setRa(new BigDecimal(ra.convertTo(Angle.Unit.DEGREES).getMagnitude()));
        this.setDec(new BigDecimal(dec.convertTo(Angle.Unit.DEGREES).getMagnitude()));
    }

    public DegDegCoordinates(final edu.gemini.model.p1.mutable.DegDegCoordinates degDeg) {
        this.setDec(degDeg.getDec());
        this.setRa(degDeg.getRa());
    }


    @Override
    public Coordinates copyAndTarget() {
        final DegDegCoordinates degDegCoordinates = new DegDegCoordinates();
        degDegCoordinates.setDec(getDec());
        degDegCoordinates.setRa(getRa());

        return degDegCoordinates;
    }

    @Override
    public edu.gemini.model.p1.mutable.Coordinates toMutable(edu.gemini.model.p1.mutable.EphemerisElement mee) {
        mee.setDegDeg(toMutable());

        return mee.getDegDeg();
    }

    @Override
    public edu.gemini.model.p1.mutable.Coordinates toMutable(SiderealTarget mst) {
        mst.setDegDeg(toMutable());

        return mst.getDegDeg();
    }

    @Override
    protected edu.gemini.model.p1.mutable.DegDegCoordinates toMutable() {
        final edu.gemini.model.p1.mutable.DegDegCoordinates degDegCoordinates = new edu.gemini.model.p1.mutable.DegDegCoordinates();
        degDegCoordinates.setDec(new BigDecimal(getDec().convertTo(Angle.Unit.DEGREES).getMagnitude()));
        degDegCoordinates.setRa(new BigDecimal(getRa().convertTo(Angle.Unit.DEGREES).getMagnitude()));

        return degDegCoordinates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DegDegCoordinates)) return false;

        DegDegCoordinates that = (DegDegCoordinates) o;

        if (dec != null ? !dec.equals(that.dec) : that.dec != null) return false;
        if (ra != null ? !ra.equals(that.ra) : that.ra != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ra != null ? ra.hashCode() : 0;
        result = 31 * result + (dec != null ? dec.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DegDegCoordinates{" +
                "ra=" + ra +
                ", dec=" + dec +
                '}';
    }


    @Override
    public String toDisplayString() {
        final StringBuilder builder = new StringBuilder("");
        final edu.gemini.shared.skycalc.Coordinates coordinates =
                new edu.gemini.shared.skycalc.Coordinates(getRa(), getDec());
        builder.append(coordinates.toString());

        return builder.toString();
//        return "RA: " + String.format("%4.4f", ra) + " Dec: " + String.format("%4.4f", dec);
    }

    public String getDisplayString() {
        return toDisplayString();
    }


    @Override
    public boolean isDegDeg() {
        return true;
    }

    @Override
    public boolean isHmsDms() {
        return false;
    }

    public Angle getDec() {
        return new Angle(dec.doubleValue(), Angle.Unit.DEGREES);
    }

    public void setDec(Angle dec) {
        this.dec = new BigDecimal(dec.getMagnitude());
    }

    public void setDec(BigDecimal dec) {
        this.dec = dec;
    }

    public Angle getRa() {
        return new Angle(ra.doubleValue(), Angle.Unit.DEGREES);
    }

    public void setRa(Angle ra) {
        this.ra = new BigDecimal(ra.getMagnitude());
    }

    public void setRa(BigDecimal ra) {
        this.ra = ra;
    }

    public String toKml() {
        return "      <coordinates>" + ra + "," + dec + ",0</coordinates>\n";
    }

    @Override
    public String toKmlLongitude() {
        return ra.toString();
    }

    @Override
    public String toKmlLatitude() {
        return dec.toString();
    }

    @Override
    public String toDot() {
        String myDot = "DegDegCoordinates_" + (getId() == null ? "NULL" : getId().toString());
        String dot = myDot + ";\n";
        return dot;
    }


    public Coordinates add(Coordinates that) {
        final Angle plusRA = this.getRa().add(that.getRa());
        final Angle plusDec = this.getDec().add(that.getDec());

        return new DegDegCoordinates(plusRA, plusDec);
    }
}
