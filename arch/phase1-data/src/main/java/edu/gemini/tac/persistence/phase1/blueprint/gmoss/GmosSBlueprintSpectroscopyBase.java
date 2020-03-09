package edu.gemini.tac.persistence.phase1.blueprint.gmoss;

import edu.gemini.model.p1.mutable.GmosSDisperser;
import edu.gemini.model.p1.mutable.GmosSFilter;
import edu.gemini.model.p1.mutable.GmosSWavelengthRegime;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
abstract public class GmosSBlueprintSpectroscopyBase extends BlueprintBase
{
    @Column(name = "nod_and_shuffle")
    protected Boolean nodAndShuffle = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "wavelength_regime")
    protected GmosSWavelengthRegime wavelengthRegime = GmosSWavelengthRegime.OPTICAL;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "filter")
    protected GmosSFilter filter = GmosSFilter.g_G0325;

    @Column(name = "disperser")
    @Enumerated(EnumType.STRING)
    protected GmosSDisperser disperser = GmosSDisperser.B1200_G5321;

    public GmosSBlueprintSpectroscopyBase() {}
    
    public GmosSBlueprintSpectroscopyBase(final String id, final String name, final GmosSWavelengthRegime regime, final GmosSDisperser disperser, final GmosSFilter filter) {
        this(id, name, new HashSet<Observation>());

        setWavelengthRegime(regime);
        setDisperser(disperser);
        setFilter(filter);
    }

    public GmosSBlueprintSpectroscopyBase(String id, String name, Set<Observation> observations) {
        super(id, name, Instrument.GMOS_S, observations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosSBlueprintSpectroscopyBase)) return false;
        if (!super.equals(o)) return false;

        GmosSBlueprintSpectroscopyBase that = (GmosSBlueprintSpectroscopyBase) o;

        if (disperser != null ? !disperser.equals(that.disperser) : that.disperser != null) return false;
        if (filter != that.filter) return false;
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
        return result;
    }

    @Override
    public String toString() {
        return "GnirsBlueprintBase{" +
                "nodAndShuffle=" + nodAndShuffle +
                ", wavelengthRegime=" + wavelengthRegime +
                ", filter=" + filter +
                ", disperser=" + disperser +
                '}';
    }

    @Override
    public boolean isMOS() {
        return false;
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        Map<String, Set<String>> resources = new HashMap<String, Set<String>>();

        resources.put("disperser", new HashSet<String>());
        resources.put("filter", new HashSet<String>());
        resources.put("wavelengthRegime", new HashSet<String>());
        resources.put("nodAndShuffle", new HashSet<String>());

        resources.get("disperser").add(disperser.value());
        resources.get("filter").add(filter.value());
        resources.get("wavelengthRegime").add(wavelengthRegime.name());
        resources.get("nodAndShuffle").add(nodAndShuffle ? "N&S" : "");

        return resources;
    }

    public Boolean getNodAndShuffle() {
        return nodAndShuffle;
    }

    public void setNodAndShuffle(Boolean nodAndShuffle) {
        this.nodAndShuffle = nodAndShuffle;
    }

    public GmosSFilter getFilter() {
        return filter;
    }

    public void setFilter(GmosSFilter filter) {
        this.filter = filter;
    }

    public GmosSDisperser getDisperser() {
        return disperser;
    }

    public void setDisperser(GmosSDisperser disperser) {
        this.disperser = disperser;
    }

    public GmosSWavelengthRegime getWavelengthRegime() {
        return wavelengthRegime;
    }

    public void setWavelengthRegime(GmosSWavelengthRegime wavelengthRegime) {
        this.wavelengthRegime = wavelengthRegime;
    }

    @Override
    public String getDisplayCamera() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayDisperser() {
        return disperser.name();
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
}
