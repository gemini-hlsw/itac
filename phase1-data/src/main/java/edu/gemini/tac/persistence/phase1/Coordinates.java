package edu.gemini.tac.persistence.phase1;

import edu.gemini.shared.skycalc.Angle;

import javax.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Table(name = "v2_coordinates")
public abstract class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public abstract Coordinates copyAndTarget();
    public abstract edu.gemini.model.p1.mutable.Coordinates toMutable(final edu.gemini.model.p1.mutable.EphemerisElement mee);
    public abstract edu.gemini.model.p1.mutable.Coordinates toMutable(final edu.gemini.model.p1.mutable.SiderealTarget mst);
    protected abstract edu.gemini.model.p1.mutable.Coordinates toMutable();

    @Override
    public String toString() {
        return "Coordinates{" +
                "id=" + id +
//                ", target=" + target +
                '}';
    }

    public String getDisplayString() {
        return toDisplayString();
    }
    public abstract String toDisplayString();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public abstract boolean isDegDeg();
    public abstract boolean isHmsDms();

    public abstract String toKml();
    public abstract String toKmlLongitude();
    public abstract String toKmlLatitude();

    public abstract String toDot();

    public abstract Angle getRa();
    public abstract Angle getDec();
    public abstract void setRa(Angle ra);
    public abstract void setDec(Angle dec);
}
