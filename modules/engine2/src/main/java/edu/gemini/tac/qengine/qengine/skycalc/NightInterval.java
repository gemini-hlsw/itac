package edu.gemini.qengine.skycalc;

import edu.gemini.shared.skycalc.Night;
import edu.gemini.shared.skycalc.SiteDesc;
import edu.gemini.shared.skycalc.TwilightBoundType;
import edu.gemini.shared.skycalc.TwilightBoundedNight;
import edu.gemini.skycalc.Interval;
import edu.gemini.skycalc.Union;
import edu.gemini.tac.qengine.ctx.Site;

import java.util.*;

/**
 * A NightInterval is an Interval that can work as a Night as well.  This class
 * implements the Night interface in terms of an Interval.  Provides methods
 * that split up an interval into any twilight bounded nights they contain.
 */
public class NightInterval extends Interval implements Night {

    private final SiteDesc site;

    public NightInterval(SiteDesc site, long start, long end) {
        super(start, end);
        this.site = site;
    }

    @Override
    protected NightInterval create(long start, long end) {
        return new NightInterval(site, start, end);
    }

    @Override
    public SiteDesc getSite() { return site; }

    @Override
    public long getStartTime() { return getStart(); }

    @Override
    public long getEndTime() { return getEnd(); }

    @Override
    public long getTotalTime() { return getLength(); }

    @Override
    public boolean includes(long l) { return contains(l); }
}
