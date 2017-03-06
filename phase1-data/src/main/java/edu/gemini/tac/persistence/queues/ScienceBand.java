package edu.gemini.tac.persistence.queues;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.Embeddable;
import java.util.*;

/**
 * http://www.gemini.edu/node/11101?q=node/11521 has useful
 * background information on science bands.  I'm reproducing some of it
 * here because I don't trust that url to not link-rot.
 *
 * The top-level principles guiding the queue management and planning are (1) use the largest possible fraction of the
 * time on completing band 1 and 2 programs, and (2) make the best use of all telescope time. These principles translate
 *  to the following concrete guidelines for the queue coordinators:
 * Within a ranking band all programs have equal priority.
 * Aim to complete started programs.
 * Do not lose early targets in band 1 and band 2 programs. This can override band ranking if needed to ensure Band 2 targets are observed.
 * Limit the execution in better than requested conditions, in order to enable completion of the more demanding band 1 and 2 programs.
 * Select Band 3 programs to start based on: probability of reaching the PI-defined minimum useful time, probability of
 *  completion, complementary to band 1 and 2 in conditions and RA, and connection to a thesis program.
 *
 * http://www.gemini.edu/sciops/observing-with-gemini/observing-modes/poor-weather
 * The "poor weather" (PW) queue is a program to fill telescope time under very poor, but usable, conditions. Time spent on these
 * programs will not be charged to the PI or the partner countries. These are queue programs only but are distinct from the "regular"
 * band 1 to band 3 queue. They will be executed only when nothing in the regular queue is observable.  In all other respects
 * (NGO and contact scientist support, science archive data distribution) they are identical to other Gemini programs.
 *
 * @author ddawson
 *
 */
@Embeddable
public class ScienceBand {
	public final static ScienceBand BAND_ONE = new ScienceBand(1, "Band One");
	public final static ScienceBand BAND_TWO = new ScienceBand(2, "Band Two");
	public final static ScienceBand BAND_THREE = new ScienceBand(3, "Band Three");
	public final static ScienceBand POOR_WEATHER = new ScienceBand(4, "Band Poor Weather");
    public final static ScienceBand CLASSICAL = new ScienceBand(5, "Classical");
    public final static ScienceBand EXCHANGE = new ScienceBand(6, "Exchange");
    public final static ScienceBand MULTIBAND = new MultiBand(7, "Composite Band");

	final private long rank;
	final private String description;

    @SuppressWarnings("unused")
	private ScienceBand() { this(-1, "Required by Hibernate"); }

	ScienceBand(final long rank, final String description) {
		this.rank = rank;
		this.description = description;
	}

	public long getRank() {
		return rank;
	}

	public String getDescription() {
		return description;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(538611, 1129308109).
			appendSuper(super.hashCode()).
			append(rank).
			append(description).
			toHashCode();
	}

	@Override 
	public boolean equals(Object o) { 
		if (!(o instanceof ScienceBand))
			return false;

		final ScienceBand rhs = (ScienceBand) o; 

		return new EqualsBuilder().
			append(rank, rhs.getRank()).
			append(description, rhs.getDescription()).
			isEquals();
	}
	
	public String toString() {
		return new ToStringBuilder(this).
			append("rank", rank).
			append("description", description).
			toString();
	}

    public static ScienceBand lookupFromRank(final int lookupRank) {
        switch(lookupRank) {
            case 1:
                return BAND_ONE;
            case 2:
                return BAND_TWO;
            case 3:
                return BAND_THREE;
            case 4:
                return POOR_WEATHER;
            case 5:
                return CLASSICAL;
            case 6:
                return EXCHANGE;
            case 7:
                return MULTIBAND;
            default:
                return null;
        }
    }

    public static List<ScienceBand> byProgramIdOrder(){
        List<ScienceBand> ordering = new ArrayList<ScienceBand>(5);
        ordering.add(CLASSICAL);
        ordering.add(BAND_ONE);
        ordering.add(BAND_TWO);
        ordering.add(BAND_THREE);
        ordering.add(POOR_WEATHER);
        ordering.add(EXCHANGE);
        ordering.add(MULTIBAND);
        return ordering;
    }
}

class MultiBand extends ScienceBand {
    MultiBand(final int rank, final String description){
        super(rank, description);
    }
}

