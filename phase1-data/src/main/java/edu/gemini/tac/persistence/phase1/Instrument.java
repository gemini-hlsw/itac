package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.ExchangePartner;
import edu.gemini.tac.persistence.Site;

public enum Instrument {
    SUBARU("Subaru", ExchangePartner.SUBARU),
    KECK("Keck", ExchangePartner.KECK),
    GMOS_N("GMOS North", Site.NORTH),
    GMOS_S("GMOS South", Site.SOUTH),
    GNIRS("GNIRS", Site.NORTH),
    MICHELLE("Michelle", Site.NORTH),
    NICI("NICI", Site.SOUTH),
    NIFS("NIFS", Site.NORTH),
    NIRI("NIRI", Site.NORTH),
    PHOENIX_GS("Phoenix Gemini South", Site.SOUTH),
    PHOENIX_GN("Phoenix Gemini North", Site.NORTH),
    FLAMINGOS2("Flamingos 2", Site.SOUTH),
    TRECS("T-ReCS", Site.SOUTH),
    GRACES("Graces", Site.NORTH),
    GSAOI("GSAOI", Site.SOUTH),
    GPI("GPI", Site.SOUTH),
    DSSI_GN("Dssi Gemini North", Site.NORTH),
    DSSI_GS("Dssi Gemini South", Site.SOUTH),
    TEXES_GN("Texes Gemini North", Site.NORTH),
    TEXES_GS("Texes Gemini South", Site.SOUTH),
    VISITORGS("Visitor Gemini North", Site.SOUTH),
    VISITORGN("Visitor Gemini South", Site.NORTH);

    private final String displayName;
    private ExchangePartner exchangePartner = null;
    private Site site = null;

    Instrument(final String displayName) {
        this.displayName = displayName;
    }

    Instrument(final String displayName, final ExchangePartner exchangePartner) {
        this(displayName);
        this.exchangePartner = exchangePartner;
    }

    Instrument(final String displayName, final Site site) {
        this(displayName);
        this.site = site;
    }

    public static Instrument fromValue(final Instrument v) {
        for (Instrument c: Instrument.values()) {
            if (c.name().equals(v.name())) {
                return c;
            }
        }
        throw new IllegalArgumentException(v.toString());
    }

    @Override
    public String toString() {
        return "Instrument{" +
                "displayName='" + displayName + '\'' +
                ", exchangePartner=" + exchangePartner +
                ", site=" + site +
                '}';
    }

    public String getDisplayName() {
        return displayName;
    }

    public ExchangePartner getExchangePartner() {
        return exchangePartner;
    }

    public Site getSite() {
        return site;
    }

    public boolean isExchange() { return getExchangePartner() != null; }
    public boolean isGemini() { return site != null; }
}
