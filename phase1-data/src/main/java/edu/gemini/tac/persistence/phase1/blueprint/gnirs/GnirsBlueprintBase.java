package edu.gemini.tac.persistence.phase1.blueprint.gnirs;

import edu.gemini.model.p1.mutable.AltairChoice;
import edu.gemini.model.p1.mutable.GnirsPixelScale;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.altair.AltairConfiguration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
abstract public class GnirsBlueprintBase extends BlueprintBase {
    private static final String CAMERA_FORMAT = "%s (%s arcsec)";

    @Enumerated(EnumType.STRING)
    @Column(name = "pixel_scale")
    protected GnirsPixelScale pixelScale = GnirsPixelScale.PS_005;

    @Enumerated(EnumType.STRING)
    @Column(name = "altair_configuration")
    protected AltairConfiguration altairConfiguration = AltairConfiguration.NONE;

    protected GnirsBlueprintBase() {}

    public GnirsBlueprintBase(final String id, final String name, final Set<Observation> observations) {
        super(id, name, Instrument.GNIRS, observations);
    }

    public GnirsBlueprintBase(final String id, final String name, final AltairChoice altairChoice, final GnirsPixelScale pixelScale, final Set<Observation> observations) {
        this(id, name, observations);
        setAltairConfiguration(AltairConfiguration.fromAltairChoice(altairChoice));
        setPixelScale(pixelScale);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GnirsBlueprintBase)) return false;
        if (!super.equals(o)) return false;

        GnirsBlueprintBase that = (GnirsBlueprintBase) o;

        if (altairConfiguration != that.altairConfiguration) return false;
        if (pixelScale != that.pixelScale) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pixelScale != null ? pixelScale.hashCode() : 0);
        result = 31 * result + (altairConfiguration != null ? altairConfiguration.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GnirsBlueprintBase{" +
                "pixelScale=" + pixelScale +
                ", altairConfiguration=" + altairConfiguration +
                '}';
    }

    @Override
    public boolean isMOS() {
        return false;
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        Map<String, Set<String>> resources = new HashMap<String, Set<String>>();

        resources.put("pixelScale", new HashSet<String>());
        resources.put("altairConfiguration", new HashSet<String>());

        resources.get("pixelScale").add(pixelScale.value());
        resources.get("altairConfiguration").add(altairConfiguration.value());

        return resources;
    }


    public GnirsPixelScale getPixelScale() {
        return pixelScale;
    }

    public void setPixelScale(GnirsPixelScale pixelScale) {
        this.pixelScale = pixelScale;
    }

    public AltairConfiguration getAltairConfiguration() {
        return altairConfiguration;
    }

    public void setAltairConfiguration(AltairConfiguration altairConfiguration) {
        this.altairConfiguration = altairConfiguration;
    }

    @Override
    public String getDisplayCamera() {
        switch(pixelScale) {
            case PS_005:
                return String.format(CAMERA_FORMAT, "long", pixelScale.value().subSequence(0, 4));
            case PS_015:
                return String.format(CAMERA_FORMAT, "short", pixelScale.value().subSequence(0, 4));
            default:
                return pixelScale.value();
        }
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        return getAltairConfiguration().getDisplayValue();
    }

    @Override
    public boolean hasLgs() {
        switch (altairConfiguration) {
            case LGS_WITH_PWFS1:
            case LGS_WITHOUT_PWFS1:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean hasAltair() {
        if (altairConfiguration == AltairConfiguration.NONE) {
            return false;
        } else {
            return true;
        }
    }
}
