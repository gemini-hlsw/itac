package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.Guider;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "v2_guide_stars")
@BatchSize(size = 500)
public class GuideStar {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "guider")
    protected Guider guider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observation_id")
    protected Observation observation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Target target ;

    public GuideStar() {}

    public GuideStar(final GuideStar copied, final Observation observation) {
        setObservation(observation);
        setGuider(copied.getGuider());
        setTarget(observation.getTarget());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuideStar)) return false;

        GuideStar guideStar = (GuideStar) o;

        if (guider != guideStar.guider) return false;
        if (target != null ? !target.equals(guideStar.target) : guideStar.target != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = guider != null ? guider.hashCode() : 0;
        result = 31 * result + (target != null ? target.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GuideStar{" +
                "id=" + id +
                ", guider=" + guider +
                ", target=" + target +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Guider getGuider() {
        return guider;
    }

    public void setGuider(Guider guider) {
        this.guider = guider;
    }

    public Observation getObservation() {
        return observation;
    }

    public void setObservation(Observation observation) {
        this.observation = observation;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }
}
