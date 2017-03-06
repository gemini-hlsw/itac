package edu.gemini.tac.persistence.phase1;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class SuccessEstimation {
    @Column(name = "success_estimation_value")
    protected BigDecimal value;

    @Column(name = "success_estimation_checksum")
    protected String checksum;

    @Override
    public String toString() {
        return "SuccessEstimation{" +
                "value=" + value +
                ", checksum='" + checksum + '\'' +
                '}';
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
