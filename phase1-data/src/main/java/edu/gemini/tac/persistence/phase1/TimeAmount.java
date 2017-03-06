package edu.gemini.tac.persistence.phase1;

import org.w3c.dom.Element;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.math.BigDecimal;

@Embeddable
public class TimeAmount implements Comparable {
    @Column(name = "time_amount_value", nullable = false)
    protected BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_amount_unit", nullable = false)
    protected TimeUnit units;
    private static final double EQUALS_TOLERANCE = 0.00001d;

    public TimeAmount(final BigDecimal value, final TimeUnit units) {
        this.value = value;
        this.units = units;
    }

    public TimeAmount(final long value, final TimeUnit units) {
        this(new BigDecimal(value), units);
    }

    public TimeAmount(final double value, final TimeUnit units) {
        this(new BigDecimal(value), units);
    }

    public TimeAmount(final edu.gemini.model.p1.mutable.TimeAmount timeAmount) {
        this.value = timeAmount.getValue();
        this.units = TimeUnit.fromValue(timeAmount.getUnits().value());
    }

    public TimeAmount(final TimeAmount that) {
        this(that.getValue(), that.getUnits());
    }

    /**
     * text content epoch a la Rollover Service <time>123456</time>
     * @param timeNode
     */
    public TimeAmount(Element timeNode) {
        long ms = Long.parseLong(timeNode.getTextContent());
        value = new BigDecimal((double)ms / (1000 * 60));
        units = TimeUnit.MIN;
    }

    public edu.gemini.model.p1.mutable.TimeAmount toMutable() {
        final edu.gemini.model.p1.mutable.TimeAmount mTimeAmount = new edu.gemini.model.p1.mutable.TimeAmount();

        if (getUnits().equals(TimeUnit.MIN)) {
            final double conversionFactor = TimeUnit.conversionFactor(TimeUnit.MIN, TimeUnit.HR);
            mTimeAmount.setValue(getValue().multiply(new BigDecimal(conversionFactor)));
            mTimeAmount.setUnits(edu.gemini.model.p1.mutable.TimeUnit.HR);
        }
        else {
            mTimeAmount.setValue(getValue());
            mTimeAmount.setUnits(edu.gemini.model.p1.mutable.TimeUnit.fromValue(getUnits().value()));
        }

        return mTimeAmount;
    }

    protected TimeAmount() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeAmount)) return false;

        TimeAmount that = (TimeAmount) o;

        if (units != that.units) return false;
        if (value == null || that.getValue() == null)
            return false;

        if (Math.abs(value.doubleValue() - that.getValue().doubleValue()) > EQUALS_TOLERANCE)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (units != null ? units.hashCode() : 0);
        return result;
    }

    public BigDecimal getValue() {
        return value;
    }

    public TimeUnit getUnits() {
        return units;
    }

    /**
     * Aids in conversion between times of different units.
     *
     * @param units
     * @return a new TimeAmount in the equivalent to this in the scale of the units parameter
     */
    public TimeAmount convertTo(final TimeUnit units) {
        if (this.units == units)
            return new TimeAmount(this.value, this.units);

        final double conversionFactor = TimeUnit.conversionFactor(this.units, units);

        return new TimeAmount(value.multiply(new BigDecimal(conversionFactor)), units);
    }

    public String toPrettyString() {
        return String.format("%.2f %s", value.floatValue(), units.toString());
    }

    public String getPrettyString() {
        return toPrettyString();
    }

    public TimeAmount sum(final TimeAmount other) {
        return new TimeAmount(value.add(other.convertTo(this.getUnits()).getValue()), this.getUnits());
    }

    public double getDoubleValue() {
        return value.doubleValue();
    }

    public double getDoubleValueInHours() {
        return convertTo(TimeUnit.HR).getDoubleValue();
    }

    @Override
    public int compareTo(Object o) {
        TimeAmount that = ((TimeAmount) o).convertTo(this.getUnits());
        return this.getValue().compareTo(that.getValue());
    }

    public BigDecimal getValueInHours() {
        return convertTo(TimeUnit.HR).getValue();
    }

    public edu.gemini.model.p1.mutable.TimeAmount toPhase1(){
        edu.gemini.model.p1.mutable.TimeAmount t = new edu.gemini.model.p1.mutable.TimeAmount();
        t.setValue(this.getValue());
        t.setUnits(edu.gemini.model.p1.mutable.TimeUnit.fromValue(this.getUnits().value()));
        return t;
    }
}
