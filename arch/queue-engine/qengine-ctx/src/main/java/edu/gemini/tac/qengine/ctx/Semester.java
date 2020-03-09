//
// $
//

package edu.gemini.tac.qengine.ctx;

import java.io.Serializable;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semester information, a year and a letter (A or B) designating which half
 * of the year we are in.
 */
public final class Semester implements Comparable<Semester>, Serializable {

    /**
     * Enumeration of the two semester halves for each year.
     */
    public enum Half {
        A(Calendar.FEBRUARY) {
            public Half opposite() { return B; }
            public int prev(int year) { return year-1; }
            public int next(int year) { return year;   }
        },
        B(Calendar.AUGUST) {
            public Half opposite() { return A; }
            public int prev(int year) { return year;   }
            public int next(int year) { return year+1; }
        },
        ;

        private final int startMonth;

        Half(int startMonth) {
            this.startMonth = startMonth;
        }

        public int getStartMonth() { return startMonth; }

        public abstract Half opposite();
        public abstract int next(int year);
        public abstract int prev(int year);

        /**
         * Gets the Semester Half that corresponds to the given Java
         * calendar month (which is zero based).
         *
         * @param javaCalendarMonth zero-based month
         * (January = 0, December = 11); use Calendar.JANUARY, etc.
         *
         * @return Semester half that corresponds to the given month
         */
        public static Half forMonth(int javaCalendarMonth) {
            return (javaCalendarMonth >= A.startMonth) &&
                   (javaCalendarMonth  < B.startMonth) ? A : B;
        }

        public Date getStartDate(Site site, int year) {
            final Calendar cal = mkCalendar(site);
            //noinspection MagicConstant
            cal.set(year, startMonth, 1, 14, 0, 0);
            cal.add(Calendar.DAY_OF_MONTH, -1);
            return cal.getTime();
        }

        public Date getEndDate(Site site, int year) {
            final Calendar cal = mkCalendar(site);
            cal.setTime(getStartDate(site, year));
            cal.add(Calendar.MONTH, 6);
            return cal.getTime();
        }
    }

    private static Calendar mkCalendar(Site site) {
        Calendar cal = new GregorianCalendar(site.timeZone());
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private final int year;
    private final Half half;

    /**
     * Constructs a semester that corresponds to the current date.
     */
    public Semester(Site site) {
        this(site, new Date());
    }

    /**
     * Constructs the semester in which the given date falls.
     *
     * @param time an instant contained in the Semester to construct
     */
    public Semester(Site site, Date time) {
        final Calendar cal = mkCalendar(site);
        cal.setTime(time);

        int year = cal.get(Calendar.YEAR);
        Half half = Half.B;

        final long timestamp = time.getTime();
        while (timestamp < half.getStartDate(site, year).getTime()) {
            year = half.prev(year);
            half = half.opposite();
        }

        this.year = year;
        this.half = half;
    }

    /**
     * Constructs by explicitly providing the year and the semester half.
     */
    public Semester(int year, Half half) {
        if (half == null) throw new NullPointerException("half = null");
        this.year = year;
        this.half = half;
    }

    public int getYear() { return year; }
    public Half getHalf() { return half; }

    @Override
    public int compareTo(Semester that) {
        int res = year - that.year;
        if (res != 0) return res;
        return half.compareTo(that.half);
    }

    public Date getStartDate(Site site) {
        return half.getStartDate(site, year);
    }

    public Date getEndDate(Site site) {
        return half.getEndDate(site, year);
    }

    /* Returns the date halfway between the start and end dates */
    public Date getMidpoint(Site site) {
        final Date start = getStartDate(site);
        final Date end = getEndDate(site);
        final long midEpoch = start.getTime() + (end.getTime() - start.getTime()) / 2;
        return new Date(midEpoch);
    }

    public Semester previous() {
        return new Semester(half.prev(year), half.opposite());
    }

    public Semester next() {
        return new Semester(half.next(year), half.opposite());
    }

    @Override
    public String toString() { return String.format("%d%s", year, half); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Semester semester = (Semester) o;

        if (year != semester.year) return false;
        if (half != semester.half) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = year;
        result = 31 * result + half.hashCode();
        return result;
    }

    private static Pattern PAT = Pattern.compile("(\\d\\d\\d\\d)-?([AB])");

    public static Semester parse(String semesterStr) throws ParseException {
        final Matcher m = PAT.matcher(semesterStr);
        if (!m.matches()) {
            // I think I would rather return None :-/
            throw new ParseException(semesterStr,0);
        }
        return new Semester(Integer.parseInt(m.group(1)), Half.valueOf(m.group(2)));
    }
}
