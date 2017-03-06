package edu.gemini.tac.persistence.phase1.submission;

import edu.gemini.tac.persistence.phase1.Investigator;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Map;

@Embeddable
public class SubmissionRequest {
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "request_value")),
            @AttributeOverride(name = "units", column = @Column(name = "request_units"))
    })
    protected TimeAmount time;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "request_min_value")),
            @AttributeOverride(name = "units", column = @Column(name = "request_min_units"))
    })
    protected TimeAmount minTime;

    public SubmissionRequest() {
    }

    public SubmissionRequest(final SubmissionRequest copiedRequest) {
        if (copiedRequest != null) {
            setTime(new TimeAmount(copiedRequest.getTime()));
            setMinTime(new TimeAmount(copiedRequest.getMinTime()));
        }
    }

    public SubmissionRequest(TimeAmount time, TimeAmount minTime) {
        this.time = time;
        this.minTime = minTime;
    }

    public SubmissionRequest(final edu.gemini.model.p1.mutable.SubmissionRequest mRequest) {
        setMinTime(new TimeAmount(mRequest.getMinTime()));
        setTime(new TimeAmount(mRequest.getTime()));
    }

    public void setTime(TimeAmount time) {
        this.time = time;
    }

    public void setMinTime(TimeAmount minTime) {
        this.minTime = minTime;
    }

    public TimeAmount getTime() {
        return time;
    }

    public TimeAmount getMinTime() {
        return minTime;
    }

    public edu.gemini.model.p1.mutable.SubmissionRequest toMutable() {
        final edu.gemini.model.p1.mutable.SubmissionRequest mRequest = new edu.gemini.model.p1.mutable.SubmissionRequest();
        mRequest.setMinTime(getMinTime().toMutable());
        mRequest.setTime(getTime().toMutable());

        return mRequest;
    }
}
