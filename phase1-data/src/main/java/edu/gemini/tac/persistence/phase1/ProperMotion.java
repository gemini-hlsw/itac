package edu.gemini.tac.persistence.phase1;

import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "v2_proper_motions")
@BatchSize(size = 500)
public class ProperMotion {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "delta_ra")
    protected BigDecimal deltaRA;

    @Column(name = "delta_dec")
    protected BigDecimal deltaDec;

    public ProperMotion() {}
    public ProperMotion(final edu.gemini.model.p1.mutable.ProperMotion properMotion) {
        this.deltaRA = properMotion.getDeltaRA();
        this.deltaDec = properMotion.getDeltaDec();
    }

    public edu.gemini.model.p1.mutable.ProperMotion toMutable() {
        final edu.gemini.model.p1.mutable.ProperMotion motion = new edu.gemini.model.p1.mutable.ProperMotion();
        motion.setDeltaDec(getDeltaDec());
        motion.setDeltaRA(getDeltaRA());

        return motion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProperMotion)) return false;

        ProperMotion that = (ProperMotion) o;

        if (deltaDec != null ? !deltaDec.equals(that.deltaDec) : that.deltaDec != null) return false;
        if (deltaRA != null ? !deltaRA.equals(that.deltaRA) : that.deltaRA != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = deltaRA != null ? deltaRA.hashCode() : 0;
        result = 31 * result + (deltaDec != null ? deltaDec.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ProperMotion{" +
                "id=" + id +
                ", deltaRA=" + deltaRA +
                ", deltaDec=" + deltaDec +
                '}';
    }

    public BigDecimal getDeltaRA() {
        return deltaRA;
    }

    public void setDeltaRA(BigDecimal value) {
        this.deltaRA = value;
    }

    public BigDecimal getDeltaDec() {
        return deltaDec;
    }

    public void setDeltaDec(BigDecimal value) {
        this.deltaDec = value;
    }

}
