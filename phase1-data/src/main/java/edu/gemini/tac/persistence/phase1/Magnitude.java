package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.MagnitudeBand;
import edu.gemini.model.p1.mutable.MagnitudeSystem;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "v2_magnitudes")
public class Magnitude {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "value")
    protected BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "band", nullable = false)
    protected MagnitudeBand band;

    @Enumerated(EnumType.STRING)
    @Column(name = "system", nullable = false)
    protected MagnitudeSystem system;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    protected Target target;

    public Magnitude() {}

    public Magnitude(final Magnitude that, final Target target) {
        this.setValue(that.getValue());
        this.setBand(that.getBand());
        this.setSystem(that.getSystem());
        this.setTarget(target);
    }

    public edu.gemini.model.p1.mutable.Magnitude toMutable() {
        final edu.gemini.model.p1.mutable.Magnitude mm = new edu.gemini.model.p1.mutable.Magnitude();
        mm.setBand(getBand());
        mm.setSystem(getSystem());
        mm.setValue(getValue());

        return mm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Magnitude)) return false;

        Magnitude magnitude = (Magnitude) o;

        if (band != magnitude.band) return false;
        if (system != magnitude.system) return false;
        if (value != null ? !value.equals(magnitude.value) : magnitude.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (band != null ? band.hashCode() : 0);
        result = 31 * result + (system != null ? system.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Magnitude{" +
                "id=" + id +
                ", value=" + value +
                ", band=" + band +
                ", system=" + system +
                '}';
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public MagnitudeBand getBand() {
        return band;
    }

    public void setBand(MagnitudeBand band) {
        this.band = band;
    }

    public MagnitudeSystem getSystem() {
        return system;
    }

    public void setSystem(MagnitudeSystem system) {
        this.system = system;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public String toDisplayString(){
        return String.format("%4.4f",value.doubleValue()) + " " + band + " (" + system + ")";
    }

    public String getDisplayString() {
        return toDisplayString();
    }
}
