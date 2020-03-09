package edu.gemini.tac.persistence.bin;

import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.*;

/**
 * HH:MM:SS.SS where HH are hours 00-23, MM are minutes 00-59, SS are seconds 00-59 with fractions of seconds
 * 
 * @author ddawson
 *
 */
@Entity
@Table(name = "ra_bin_sizes")
public class RABinSize {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "hours")
    private Integer hours;

    @Column(name = "minutes")
    private Integer minutes;

    public final Integer getHours() {
        return hours;
    }
    public final void setHours(Integer hours) {
        this.hours = hours;
    }
    public final Integer getMinutes() {
        return minutes;
    }
    public final void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }
    public String getDisplay() {
        return String.format("%02d:%02d", hours, minutes);
    }

    public Integer getSizeAsMinutes() {
        return (60 * hours) + minutes;
    }

    public String toString() {
        return new ToStringBuilder(this).
            append("hours", hours).
            append("minutes", minutes).
            toString();
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
}
