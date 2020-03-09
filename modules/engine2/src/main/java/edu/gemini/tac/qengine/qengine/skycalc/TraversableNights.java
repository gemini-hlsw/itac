/*
 * Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
 * For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause
 */

package edu.gemini.qengine.skycalc;

import edu.gemini.skycalc.Night;
import edu.gemini.skycalc.TwilightBoundType;
import edu.gemini.spModel.core.Semester;
import edu.gemini.spModel.core.Site;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Collection of continuous nights specified by start and end time boundaries.
 */
public class TraversableNights implements Iterable<Night> {
    private final Site site;
    private final Date start;
    private final Date end;
    private final TwilightBoundType twilightBoundType;

    public TraversableNights(Site site, Semester semester) {
        this(site, semester, TwilightBoundType.NAUTICAL);
    }

    public TraversableNights(Site site, Semester semester, TwilightBoundType twilight) {
        this(site, semester.getStartDate(site), semester.getEndDate(site), twilight);
    }

    public TraversableNights(Site site, Date start, Date end) {
        this(site, start, end, TwilightBoundType.NAUTICAL);
    }

    public TraversableNights(Site site, Date start, Date end, TwilightBoundType twilight) {
        this.site              = site;
        this.start             = start;
        this.end               = end;
        this.twilightBoundType = twilight;
    }

    public Site getSite() { return site; }
    public Date getStart() { return start; }
    public Date getEnd() { return end; }
    public TwilightBoundType getTwilightBoundType() { return twilightBoundType; }

    public Iterator<Night> iterator() {
        return new NightIterator(site, start, end, twilightBoundType);
    }

    public List<Night> toList() {
        List<Night> lst = new ArrayList<Night>(190);
        for (Night n : this) lst.add(n);
        return lst;
    }

    public long totalTime() {
        long res = 0;
        for (Night n : this) res += n.getTotalTime();
        return res;
    }
}
