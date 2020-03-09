package edu.gemini.tac.persistence.phase1.submission;

import edu.gemini.tac.persistence.phase1.TimeAmount;

import javax.persistence.*;
import java.math.BigDecimal;

@Embeddable
public class SubmissionAccept {
    @Column(name ="accept_email")
    protected String email;

    @Column(name = "accept_ranking")
    protected BigDecimal ranking;

    @Embedded
    @AttributeOverrides( {
            @AttributeOverride(name="value", column = @Column(name="accept_recommend_value") ),
            @AttributeOverride(name="units", column = @Column(name="accept_recommend_units") )
    } )
    protected TimeAmount recommend;

    @Embedded
    @AttributeOverrides( {
            @AttributeOverride(name="value", column = @Column(name="accept_recommend_min_value") ),
            @AttributeOverride(name="units", column = @Column(name="accept_recommend_min_units") )
    } )
    protected TimeAmount minRecommend;

    @Column(name = "accept_poor_weather")
    protected Boolean poorWeather = Boolean.FALSE;

    public SubmissionAccept() {}

    public SubmissionAccept(final SubmissionAccept copiedAccept) {
        setEmail(copiedAccept.getEmail());
        setRanking(copiedAccept.getRanking());
        setRecommend(new TimeAmount(copiedAccept.getRecommend()));
        setMinRecommend(new TimeAmount(copiedAccept.getMinRecommend()));
        setPoorWeather(copiedAccept.isPoorWeather());
    }

    public SubmissionAccept(TimeAmount recommend, TimeAmount minRecommend, String email, boolean isPoorWeather, BigDecimal ranking) {
        this.recommend = recommend;
        this.minRecommend = minRecommend;
        this.email = email;
        this.poorWeather = isPoorWeather;
        this.ranking = ranking;
    }

    public SubmissionAccept(final edu.gemini.model.p1.mutable.SubmissionAccept mAccept) {
        this(new TimeAmount(mAccept.getRecommend()), new TimeAmount(mAccept.getMinRecommend()),
                mAccept.getEmail(), mAccept.isPoorWeather(), mAccept.getRanking());
    }

    @Override
    public String toString() {
        return "SubmissionAccept{" +
                "email='" + email + '\'' +
                ", ranking=" + ranking +
                ", recommend=" + recommend +
                ", minRecommend=" + minRecommend +
                ", poorWeather=" + poorWeather +
                '}';
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRanking(BigDecimal ranking) {
        this.ranking = ranking;
    }

    public void setRecommend(TimeAmount recommend) {
        this.recommend = recommend;
    }

    public void setMinRecommend(TimeAmount minRecommend) {
        this.minRecommend = minRecommend;
    }

    public void setPoorWeather(boolean poorWeather) {
        this.poorWeather = poorWeather;
    }

    public String getEmail() {
        return email;
    }

    public BigDecimal getRanking() {
        return ranking;
    }

    public TimeAmount getRecommend() {
        return recommend;
    }

    public TimeAmount getMinRecommend() {
        return minRecommend;
    }

    public boolean isPoorWeather() {
        return poorWeather;
    }

    public edu.gemini.model.p1.mutable.SubmissionAccept toMutable() {
        final edu.gemini.model.p1.mutable.SubmissionAccept mAccept = new edu.gemini.model.p1.mutable.SubmissionAccept();

        mAccept.setPoorWeather(isPoorWeather());
        mAccept.setRanking(getRanking());
        mAccept.setEmail(getEmail());
        mAccept.setMinRecommend(getMinRecommend().toMutable());
        mAccept.setRecommend(getRecommend().toMutable());

        return mAccept;
    }
}
