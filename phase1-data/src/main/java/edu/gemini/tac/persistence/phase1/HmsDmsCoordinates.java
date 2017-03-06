package edu.gemini.tac.persistence.phase1;

import edu.gemini.shared.skycalc.Angle;
import edu.gemini.shared.skycalc.DDMMSS;
import edu.gemini.shared.skycalc.HHMMSS;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.text.ParseException;

@Entity
@DiscriminatorValue("HmsDms")
@BatchSize(size = 500)
public class HmsDmsCoordinates extends Coordinates {
    @Transient
    final Logger LOGGER = Logger.getLogger(HmsDmsCoordinates.class.toString());

    @Column(name = "hms_ra")
    protected String ra;

    @Column(name = "dms_dec")
    protected String dec;

    public HmsDmsCoordinates() {}

    public HmsDmsCoordinates(final edu.gemini.model.p1.mutable.HmsDmsCoordinates hmsDms) {
        super();

        this.setDec(hmsDms.getDec());
        this.setRa(hmsDms.getRa());
    }

    @Override
    public Coordinates copyAndTarget() {
        final HmsDmsCoordinates hmsDmsCoordinates = new HmsDmsCoordinates();
        hmsDmsCoordinates.setDec(getDec());
        hmsDmsCoordinates.setRa(getRa());

        return hmsDmsCoordinates;
    }

    @Override
    public edu.gemini.model.p1.mutable.Coordinates toMutable(edu.gemini.model.p1.mutable.EphemerisElement mee) {
        mee.setHmsDms(toMutable());

        return mee.getHmsDms();
    }

    @Override
    public edu.gemini.model.p1.mutable.Coordinates toMutable(edu.gemini.model.p1.mutable.SiderealTarget mst) {
        mst.setHmsDms(toMutable());

        return mst.getHmsDms();
    }

    @Override
    protected edu.gemini.model.p1.mutable.HmsDmsCoordinates toMutable() {
        final edu.gemini.model.p1.mutable.HmsDmsCoordinates coordinates = new edu.gemini.model.p1.mutable.HmsDmsCoordinates();
        coordinates.setDec(DDMMSS.valStr(this.getDec().convertTo(Angle.Unit.DEGREES).getMagnitude()));
        coordinates.setRa(HHMMSS.valStr(this.getRa().convertTo(Angle.Unit.DEGREES).getMagnitude()));

        return coordinates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HmsDmsCoordinates)) return false;

        HmsDmsCoordinates that = (HmsDmsCoordinates) o;

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
        return "HmsDmsCoordinates{" +
                "ra=" + ra +
                ", dec=" + dec +
                '}';
    }

    @Override
    public String toDisplayString(){
        return "RA: " + ra + " Dec: " + dec;
    }

    public String getDisplayString() {
        return toDisplayString();
    }

    @Override
    public boolean isDegDeg() {
        return false;
    }

    @Override
    public boolean isHmsDms() {
        return true;
    }

    public Angle getRa() {
        try{
            return HHMMSS.parse(ra, ":");
        }catch(Exception pe){
            LOGGER.log(Level.ERROR, "Failed to parse HmsDmsCoordinate RA value " + ra);
            return null;
        }
    }

    public void setRa(String ra) {
        try{
            this.ra = ra.trim();
            //Validate the string
            HHMMSS.parse(this.ra, ":");
        }catch(ParseException pe){
            LOGGER.log(Level.ERROR, "Could not parse RA string \'" + ra + "\'");
            throw new IllegalArgumentException("Invalid RA string " + ra);
        }
    }

    public void setRa(Angle ra){
        this.ra = HHMMSS.valStr(ra.toDegrees().getMagnitude());
    }

    public Angle getDec() {
        try{
            return DDMMSS.parse(dec, ":");
        }catch(Exception pe){
            LOGGER.log(Level.ERROR, "Failed to parse HmsDmsCoordinate Dec value " + dec);
            return null;
        }
    }

    public void setDec(String dec) {
        try{
            //Validate it
            DDMMSS.parse(dec.trim());
            this.dec = dec.trim();
        }catch(ParseException pe){
            LOGGER.log(Level.ERROR, "Could not parse DEC string \'" + dec + "\'");
            throw new IllegalArgumentException("Invalid DEC string " + dec);
        }
    }

    public void setDec(Angle a){
        this.dec = DDMMSS.valStr(a.toDegrees().getMagnitude());
    }

    public String toKml() {
        return "      <coordinates>" + ra + "," + dec + ",0</coordinates>\n";
    }

    @Override
    public String toKmlLongitude() {
        return ra;
    }

    @Override
    public String toKmlLatitude() {
        return dec;
    }

    @Override
    public String toDot(){
        String myDot = "HmsDmsCoordinates_" + (getId() == null ? "NULL" : getId().toString());
        String dot = myDot + ";\n";
        return dot;
    }

}
