//
// $
//

package edu.gemini.tac.qengine.ctx;

import java.io.Serializable;
import java.util.Date;

/**
 * Combination of Site and Semester.
 */
public final class Context implements Comparable<Context>, Serializable {
    private final Site site;
    private final Semester semester;

    public Context(Site site, Semester semester) {
        if (site == null) throw new NullPointerException("site = null");
        if (semester == null) throw new NullPointerException("semester = null");

        this.site     = site;
        this.semester = semester;
    }

    public Site getSite() { return site; }
    public Semester getSemester() { return semester; }

    public Date getStartDate() { return semester.getStartDate(site); }
    public Date getMidpoint() { return semester.getMidpoint(site); }
    public Date getEndDate() { return semester.getEndDate(site); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Context context = (Context) o;

        if (!semester.equals(context.semester)) return false;
        if (site != context.site) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = site.hashCode();
        result = 31 * result + semester.hashCode();
        return result;
    }

    @Override
    public int compareTo(Context that) {
        int res = semester.compareTo(that.semester);
        if (res != 0) return res;
        return site.compareTo(that.site);
    }
}
