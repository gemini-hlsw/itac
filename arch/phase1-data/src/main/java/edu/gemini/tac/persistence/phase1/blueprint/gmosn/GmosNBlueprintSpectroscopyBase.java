package edu.gemini.tac.persistence.phase1.blueprint.gmosn;

import edu.gemini.model.p1.mutable.AltairChoice;
import edu.gemini.model.p1.mutable.GmosNDisperser;
import edu.gemini.model.p1.mutable.GmosNFilter;
import edu.gemini.model.p1.mutable.GmosNWavelengthRegime;
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
abstract public class GmosNBlueprintSpectroscopyBase extends BlueprintBase
{
    @Column(name = "nod_and_shuffle")
    protected Boolean nodAndShuffle = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "wavelength_regime")
    protected GmosNWavelengthRegime wavelengthRegime = GmosNWavelengthRegime.OPTICAL;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "filter")
    protected GmosNFilter filter = GmosNFilter.DS920_G0312;

    @Column(name = "disperser")
    @Enumerated(EnumType.STRING)
    protected GmosNDisperser disperser = GmosNDisperser.B1200_G5301;

    @Enumerated(EnumType.STRING)
    @Column(name = "altair_configuration")
    protected AltairConfiguration altairConfiguration = AltairConfiguration.NONE;

    public GmosNBlueprintSpectroscopyBase() {
    }

    public GmosNBlueprintSpectroscopyBase(final String id, final String name, final GmosNWavelengthRegime regime, final GmosNDisperser disperser, final GmosNFilter filter, AltairChoice altair) {
        this(id, name, regime, disperser, filter, new HashSet<>(), altair);
    }

    public GmosNBlueprintSpectroscopyBase(final String id, final String name, final GmosNWavelengthRegime regime, final GmosNDisperser disperser, final GmosNFilter filter, final Set<Observation> observations, AltairChoice altair){
        super(id, name, Instrument.GMOS_N, observations);

        setWavelengthRegime(regime);
        setDisperser(disperser);
        setFilter(filter);
        setAltairConfiguration(AltairConfiguration.fromAltairChoice(altair));
    }

    public GmosNBlueprintSpectroscopyBase(final String id, final String name) {
        this(id, name, new HashSet<>());
    }

    public GmosNBlueprintSpectroscopyBase(final String id, final String name, final Set<Observation> observations){
        super(id, name, Instrument.GMOS_N, observations);
    }

    @Override
    public boolean isMOS() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosNBlueprintSpectroscopyBase)) return false;
        if (!super.equals(o)) return false;

        GmosNBlueprintSpectroscopyBase that = (GmosNBlueprintSpectroscopyBase) o;

        if (disperser != null ? !disperser.equals(that.disperser) : that.disperser != null) return false;
        if (filter != that.filter) return false;
        if (altairConfiguration != that.altairConfiguration) return false;
        if (nodAndShuffle != null ? !nodAndShuffle.equals(that.nodAndShuffle) : that.nodAndShuffle != null)
            return false;
        if (wavelengthRegime != that.wavelengthRegime) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nodAndShuffle != null ? nodAndShuffle.hashCode() : 0);
        result = 31 * result + (wavelengthRegime != null ? wavelengthRegime.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + (disperser != null ? disperser.hashCode() : 0);
        result = 31 * result + (altairConfiguration != null ? altairConfiguration.hashCode() : 0);
        return result;
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        Map<String, Set<String>> resources = new HashMap<>();

        resources.put("disperser", new HashSet<>());
        resources.put("filter", new HashSet<>());
        resources.put("wavelengthRegime", new HashSet<>());
        resources.put("nodAndShuffle", new HashSet<>());
        resources.put("altairConfiguration", new HashSet<String>());

        resources.get("disperser").add(disperser.value());
        resources.get("filter").add(filter.value());
        resources.get("wavelengthRegime").add(wavelengthRegime.name());
        resources.get("nodAndShuffle").add(nodAndShuffle ? "N&S" : "");
        resources.get("altairConfiguration").add(altairConfiguration.value());

        return resources;
    }

    @Override
    public String toString() {
        return "GmosNBlueprintSpectroscopyBase{" +
                "nodAndShuffle=" + nodAndShuffle +
                ", wavelengthRegime=" + wavelengthRegime +
                ", filter=" + filter +
                ", disperser=" + disperser +
                '}';
    }

    public Boolean getNodAndShuffle() {
        return nodAndShuffle;
    }

    public void setNodAndShuffle(Boolean nodAndShuffle) {
        this.nodAndShuffle = nodAndShuffle;
    }

    public GmosNFilter getFilter() {
        return filter;
    }

    public void setFilter(GmosNFilter filter) {
        this.filter = filter;
    }

    public GmosNDisperser getDisperser() {
        return disperser;
    }

    public void setDisperser(GmosNDisperser disperser) {
        this.disperser = disperser;
    }

    public GmosNWavelengthRegime getWavelengthRegime() {
        return wavelengthRegime;
    }

    public void setWavelengthRegime(GmosNWavelengthRegime wavelengthRegime) {
        this.wavelengthRegime = wavelengthRegime;
    }

    @Override
    public String getDisplayCamera() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayDisperser() {
        return disperser.value();
    }

    @Override
    public String getDisplayFilter() {
        return getFilter().value();
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayOther() {
        return DISPLAY_NOT_APPLICABLE;
    }

    public AltairConfiguration getAltairConfiguration() {
        return altairConfiguration;
    }

    public void setAltairConfiguration(AltairConfiguration altairConfiguration) {
        this.altairConfiguration = altairConfiguration;
    }

}
