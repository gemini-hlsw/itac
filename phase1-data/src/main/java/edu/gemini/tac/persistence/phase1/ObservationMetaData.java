package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.GuidingEvaluation;
import edu.gemini.model.p1.mutable.TargetVisibility;

import javax.persistence.*;
import java.math.BigInteger;

@Embeddable
public class ObservationMetaData {
    @Embeddable
    public static class GuidingEstimation {
        protected GuidingEstimation() {}

        public GuidingEstimation(int percentage, GuidingEvaluation evaluation) {
            this.percentage = percentage;
            this.evaluation = evaluation;
        }

        public GuidingEstimation(final GuidingEstimation copied) {
            setPercentage(copied.getPercentage());
            setEvaluation(copied.getEvaluation());
        }

        @Column(name = "meta_guiding_estimation_percentage")
        protected int percentage;

        @Column(name = "meta_guiding_target_visibility")
        @Enumerated(EnumType.STRING)
        protected GuidingEvaluation evaluation;

        public edu.gemini.model.p1.mutable.GuidingEstimation toMutable() {
            final edu.gemini.model.p1.mutable.GuidingEstimation mEstimation = new edu.gemini.model.p1.mutable.GuidingEstimation();

            mEstimation.setEvaluation(getEvaluation());
            mEstimation.setPercentage(getPercentage());

            return mEstimation;
        }

        @Override
        public String toString() {
            return "GuidingEstimation{" +
                    "percentage=" + percentage +
                    ", evaluation=" + evaluation +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GuidingEstimation)) return false;

            GuidingEstimation that = (GuidingEstimation) o;

            if (percentage != that.percentage) return false;
            if (evaluation != that.evaluation) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = percentage;
            result = 31 * result + (evaluation != null ? evaluation.hashCode() : 0);
            return result;
        }

        public int getPercentage() {
            return percentage;
        }

        public void setPercentage(int percentage) {
            this.percentage = percentage;
        }

        public GuidingEvaluation getEvaluation() {
            return evaluation;
        }

        public void setEvaluation(GuidingEvaluation evaluation) {
            this.evaluation = evaluation;
        }
    }

    @Embedded
    protected GuidingEstimation guiding;

    @Column(name = "meta_target_visibility")
    @Enumerated(EnumType.STRING)
    protected TargetVisibility visibility;

    @Column(name = "meta_gsa")
    protected BigInteger gsa;

    @Column(name = "meta_checksum")
    protected String ck;

    protected ObservationMetaData() {}
    public ObservationMetaData(final ObservationMetaData copied) {
        setCk(copied.getCk());
        setGsa(copied.getGsa());
        if (copied.getGuiding() != null)
            setGuiding(new GuidingEstimation(copied.getGuiding()));
        setVisibility(copied.getVisibility());
    }
    public ObservationMetaData(GuidingEstimation guiding, TargetVisibility visibility, BigInteger gsa, String ck) {
        this.guiding = guiding;
        this.visibility = visibility;
        this.gsa = gsa;
        this.ck = ck;
    }

    @Override
    public String toString() {
        return "ObservationMetaData{" +
                "guiding=" + guiding +
                ", visibility=" + visibility +
                ", gsa=" + gsa +
                ", ck='" + ck + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObservationMetaData)) return false;

        ObservationMetaData that = (ObservationMetaData) o;

        if (ck != null ? !ck.equals(that.ck) : that.ck != null) return false;
        if (gsa != null ? !gsa.equals(that.gsa) : that.gsa != null) return false;
        if (guiding != null ? !guiding.equals(that.guiding) : that.guiding != null) return false;
        if (visibility != that.visibility) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = guiding != null ? guiding.hashCode() : 0;
        result = 31 * result + (visibility != null ? visibility.hashCode() : 0);
        result = 31 * result + (gsa != null ? gsa.hashCode() : 0);
        result = 31 * result + (ck != null ? ck.hashCode() : 0);
        return result;
    }

    public TargetVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(TargetVisibility visibility) {
        this.visibility = visibility;
    }

    public BigInteger getGsa() {
        return gsa;
    }

    public void setGsa(BigInteger gsa) {
        this.gsa = gsa;
    }

    public String getCk() {
        return ck;
    }

    public void setCk(String ck) {
        this.ck = ck;
    }

    public GuidingEstimation getGuiding() {
        return guiding;
    }

    public void setGuiding(GuidingEstimation guiding) {
        this.guiding = guiding;
    }

    public edu.gemini.model.p1.mutable.ObservationMetaData toMutable() {
        final edu.gemini.model.p1.mutable.ObservationMetaData mMeta = new edu.gemini.model.p1.mutable.ObservationMetaData();

        mMeta.setCk(getCk());
        mMeta.setGsa(getGsa());
        mMeta.setVisibility(getVisibility());
        final GuidingEstimation guidingEstimation = getGuiding();
        if (guidingEstimation != null)
            mMeta.setGuiding(guidingEstimation.toMutable());

        return mMeta;
    }
}
