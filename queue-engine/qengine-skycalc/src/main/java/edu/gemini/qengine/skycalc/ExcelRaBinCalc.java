//
// $
//

package edu.gemini.qengine.skycalc;

import edu.gemini.tac.qengine.ctx.Semester;
import edu.gemini.tac.qengine.ctx.Site;

import edu.gemini.shared.skycalc.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Calculates RaBin times based upon the method extracted from the
 * "RAdistributions.xls" spreadsheet used in the ITAC process prior to the
 * introduction of the automatted system.
 */
public final class ExcelRaBinCalc implements RaBinCalc {
    private static final TwilightBoundType DEFAULT_BOUNDS = TwilightBoundType.NAUTICAL;

    private final TwilightBoundType bounds;

    /**
     * Constructs with nautical twilight bounds.
     */
    public ExcelRaBinCalc() {
        this(DEFAULT_BOUNDS);
    }

    /**
     * @param bounds defines how twilight boundaries for evening and morning
     * are set
     */
    public ExcelRaBinCalc(TwilightBoundType bounds) {
        this.bounds = bounds;
    }

    @Override
    public List<Hours> calc(Site site, Date start, Date end, RaBinSize size) {
        SiteDesc siteDesc = SiteDescLookup.get(site);
        int binCount = size.getBinCount();

        long[] totals   = new long[binCount];
        long binSizeMs  = size.getSize() * 60 * 1000; // convert from min to ms
        long halfSizeMs = size.getSize() * 30 * 1000;

        // Skycalc lst algorithm expects a west longitude (hence -1 is
        // multiplied) expressed in hours, not degrees (hence, divide by 15).
        double longit = -1 * siteDesc.getLongitude() / 15.0;

        List<Angle> ras = size.genRas();

        Iterator<Night> it = new NightIterator(site, start, end, bounds);
        while (it.hasNext()) {
            Night night = it.next();

            long startTime = night.getStartTime();
            long endTime   = night.getEndTime();

            JulianDate jdStart = new JulianDate(new Date(startTime));
            JulianDate jdEnd   = new JulianDate(new Date(endTime));

            double eve  = Skycalc.lst(jdStart, longit);
            double morn = Skycalc.lst(jdEnd,   longit);

            for (int bin=0; bin<size.getBinCount(); ++bin) {
                double ra = ras.get(bin).toHours().getMagnitude();
                if (eve < morn) {
                    if (ra>eve && ra<morn) totals[bin] += binSizeMs;
                } else if (morn<ra) {
                    if (eve<ra) totals[bin] += binSizeMs;
                } else {
                    if (morn>ra) totals[bin] += binSizeMs;
                }
            }
        }

        List<Hours> res = new ArrayList<Hours>(binCount);
        for (int i=0; i<binCount; ++i) res.add(Hours.fromMillisec(totals[i]));
        return res;
    }

    public static void main(String[] args) throws Exception {
        RaBinSize    sz = new RaBinSize(60);
        Site       site = Site.north;
        Semester    sem = Semester.parse("2011A");
        Date      start = sem.getStartDate(site);
        Date        end = sem.getEndDate(site);
        List<Hours> hrs = (new ExcelRaBinCalc()).calc(site, start, end, sz);

        int i = 0;
        for (Hours h : hrs) {
            System.out.println(i + " " + h.getHours());
            i++;
        }

    }
}
