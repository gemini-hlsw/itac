package edu.gemini.tac.persistence.phase1.blueprint.altair;

import edu.gemini.model.p1.mutable.AltairChoice;
import edu.gemini.model.p1.mutable.AltairLGS;
import edu.gemini.model.p1.mutable.AltairNGS;
import edu.gemini.model.p1.mutable.AltairNone;

public enum AltairConfiguration {
    NONE("None"),
    NGS_WITH_FIELD_LENS("NGS with field lens"),
    NGS_WITHOUT_FIELD_LENS("NGS without field lens"),
    LGS_WITHOUT_PWFS1("LGS without PWFS1"),
    LGS_WITH_PWFS1("LGS with PWFS1");

    private final String value;

    AltairConfiguration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public String getDisplayValue() {
        if (value().equals(AltairConfiguration.NONE.value()))
            return "-";
        else
            return "Altair " + value();
    }


    public static AltairConfiguration fromValue(String v) {
        for (AltairConfiguration c: AltairConfiguration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public static AltairConfiguration fromAltairChoice(AltairChoice altairChoice) {
        if (altairChoice.getNone() != null)
            return AltairConfiguration.NONE;
        else if (altairChoice.getLgs() != null) {
            if (altairChoice.getLgs().isPwfs1() != null && altairChoice.getLgs().isPwfs1())
                return AltairConfiguration.LGS_WITH_PWFS1;
            else
                return AltairConfiguration.LGS_WITHOUT_PWFS1;
        }
        else if (altairChoice.getNgs() != null) {
            if (altairChoice.getNgs() != null && altairChoice.getNgs().isFieldLens())
                return AltairConfiguration.NGS_WITH_FIELD_LENS;
            else
                return AltairConfiguration.NGS_WITHOUT_FIELD_LENS;
        } else {
            throw new IllegalArgumentException("Unable to resolve altair choice into configuration:" + altairChoice);
        }
    }

    public AltairChoice toAltairChoice() {
        final AltairChoice altairChoice = new AltairChoice();

        switch(this) {
            case LGS_WITHOUT_PWFS1:
                altairChoice.setLgs(new AltairLGS());
                altairChoice.getLgs().setPwfs1(Boolean.FALSE);
                break;
            case LGS_WITH_PWFS1:
                altairChoice.setLgs(new AltairLGS());
                altairChoice.getLgs().setPwfs1(Boolean.TRUE);
                break;
            case NGS_WITH_FIELD_LENS:
                altairChoice.setNgs(new AltairNGS());
                altairChoice.getNgs().setFieldLens(Boolean.TRUE);
                break;
            case NGS_WITHOUT_FIELD_LENS:
                altairChoice.setNgs(new AltairNGS());
                altairChoice.getNgs().setFieldLens(Boolean.FALSE);
                break;
            case NONE:
                altairChoice.setNone(new AltairNone());
                break;
            default:
                throw new IllegalStateException("Unknown Altair Configuration encountered.  Cannot convert to altair choice." + this);
        }

        return altairChoice;
    }
}
