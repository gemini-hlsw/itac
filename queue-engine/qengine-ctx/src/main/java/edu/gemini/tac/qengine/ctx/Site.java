package edu.gemini.tac.qengine.ctx;

import java.util.TimeZone;

/**
 * Gemini site options.
 */
public enum Site {
    north("Gemini North", "GN", TimeZone.getTimeZone("Pacific/Honolulu"), new String[] {"gn", "mauna kea", "mk", "hilo", "hbf"}),
    south("Gemini South", "GS", TimeZone.getTimeZone("America/Santiago"), new String[] {"gs", "pachon", "cp", "la serena", "sbf"}),
    ;

    private final String displayValue;
    private final String abbr;
    private final TimeZone timeZone;
    private final String[] matches;

    private Site(String displayValue, String abbr, TimeZone timeZone, String[] matches) {
        this.displayValue = displayValue;
        this.abbr         = abbr;
        this.timeZone     = timeZone;
        this.matches      = matches;
    }

    public boolean matches(String desc) {
        String low = desc.toLowerCase();
        if (low.contains(name())) return true;

        if (low.contains(displayValue().toLowerCase())) return true;

        for (String m : matches) {
            if (low.contains(m)) return true;
        }
        return false;
    }

    public String displayValue() { return displayValue; }
    public String abbreviation() { return abbr; }
    public TimeZone timeZone() { return timeZone; }
    @Override public String toString() { return displayValue; }

    /**
     * Attempts to parse the given string into a Site.
     *
     * @param s site string
     *
     * @return Site that corresponds to the string, or <code>null</code>
     * otherwise
     */
    public static Site parse(String s) {
        for (Site site : values()) if (site.matches(s)) return site;
        return null;
    }
}
