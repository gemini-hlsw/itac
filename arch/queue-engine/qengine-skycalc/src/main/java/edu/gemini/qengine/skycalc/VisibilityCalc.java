package edu.gemini.qengine.skycalc;

import edu.gemini.shared.skycalc.Coordinates;
import edu.gemini.shared.skycalc.Night;
import edu.gemini.shared.skycalc.SiteDesc;
import edu.gemini.shared.skycalc.TwilightBoundedNight;
import edu.gemini.skycalc.ElevationConstraintSolver;
import edu.gemini.skycalc.Interval;
import edu.gemini.skycalc.Union;
import edu.gemini.tac.qengine.ctx.Semester;
import edu.gemini.tac.qengine.ctx.Site;

import java.beans.Visibility;
import java.util.Date;
import jsky.coords.WorldCoords;

/**
 * Visibility calculator.
 */
public final class VisibilityCalc {
    public final Site site;
    public final Date start;
    public final Date end;
    public final ElevationConfig conf;

    /**
     * Visibility calculator for the given time range using default elevation
     * constraint configuration.
     */
    public VisibilityCalc(Site site, Date start, Date end) {
        this(site, start, end, ElevationConfig.DEFAULT);
    }

    /**
     * Visibility calculator for the given time range using specific
     * constraint configuration.
     */
    public VisibilityCalc(Site site, Date start, Date end, ElevationConfig conf) {
        this.site  = site;
        this.start = start;
        this.end   = end;
        this.conf  = conf;
    }

    /**
     * Returns the number of hours that the given target is visible over the
     * date range.
     */
    public Hours hours(Coordinates target) {
        SiteDesc siteDesc = SiteDescLookup.get(site);
        WorldCoords wc    = new WorldCoords(target.getRa().toDegrees().getMagnitude(),
                                            target.getDec().toDegrees().getMagnitude());
        ElevationConstraintSolver solver;
        solver = ElevationConstraintSolver.forAirmass(siteDesc, wc, conf.getMinAirmass(), conf.getMaxAirmass());

        NightIterator itr = new NightIterator(site, start, end, conf.getBounds());

        long ms = 0;
        while (itr.hasNext()) {
            Night night = itr.next();
            ms += nightCalc(night, solver);
        }
        return Hours.fromMillisec(ms);
    }

    /**
     * Gets the total dark time over the period of the visibility calculator.
     */
    public Hours darkTime() {
        SiteDesc siteDesc = SiteDescLookup.get(site);
        NightIterator itr = new NightIterator(site, start, end, conf.getBounds());

        long ms = 0;
        while (itr.hasNext()) {
            Night night = itr.next();
            ms += night.getTotalTime();
        }
        return Hours.fromMillisec(ms);
    }

    private static long nightCalc(Night night, ElevationConstraintSolver solver) {
      long start = night.getStartTime();
      long end   = night.getEndTime();
      Union<Interval> union = solver.solve(start, end);

      long ms = 0;
      for (Interval intr : union.getIntervals()) ms += intr.getLength();
      return ms;
    }

    /**
     * Visibility calculator over the entirety of the given semester.
     */
    public static VisibilityCalc semesterCalc(Site site, Semester semester) {
      Date startDate = semester.getStartDate(site);
      Date endDate   = semester.getEndDate(site);
      return new VisibilityCalc(site, startDate, endDate);
    }

    /**
     * Visibility calculator for the time remaining in the current semester.
     */
    public static VisibilityCalc remainingTimeCalc(Site site) {
      Semester currentSemester = new Semester(site);
      Date startDate = new Date();
      Date endDate   = currentSemester.getEndDate(site);
      return new VisibilityCalc(site, startDate, endDate);
    }
}