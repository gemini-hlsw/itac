package edu.gemini.tac.persistence.phase1.blueprint.gmosn;

import edu.gemini.model.p1.mutable.GmosNBlueprintChoice;
import edu.gemini.model.p1.mutable.GmosNFilter;
import edu.gemini.model.p1.mutable.GmosNWavelengthRegime;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.altair.AltairConfiguration;
import edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintImaging;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.util.*;

@Entity
@DiscriminatorValue("GmosNBlueprintImaging")
public class GmosNBlueprintImaging
    extends BlueprintBase {

    @Enumerated(EnumType.STRING)
    @Column(name = "wavelength_regime")
    protected GmosNWavelengthRegime wavelengthRegime = GmosNWavelengthRegime.OPTICAL;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "v2_gmosn_filters",
            joinColumns = @JoinColumn(name = "blueprint_id"))
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    protected List<GmosNFilter> filters = new ArrayList<GmosNFilter>();

    @Enumerated(EnumType.STRING)
    @Column(name = "altair_configuration")
    protected AltairConfiguration altairConfiguration = AltairConfiguration.NONE;

    public GmosNBlueprintImaging() { setInstrument(Instrument.GMOS_N);}

    public GmosNBlueprintImaging(final edu.gemini.model.p1.mutable.GmosNBlueprintImaging mBlueprint, final GmosNWavelengthRegime regime) {
        super(mBlueprint.getId(), mBlueprint.getName(), Instrument.GMOS_N);

        getFilters().addAll(mBlueprint.getFilter());
        setAltairConfiguration(AltairConfiguration.fromAltairChoice(mBlueprint.getAltair()));
        setWavelengthRegime(regime);
    }

    public GmosNBlueprintImaging(GmosSBlueprintImaging complement){
        super(complement.getBlueprintId(), complement.getName(), Instrument.GMOS_N, complement.getObservations());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosNBlueprintImaging)) return false;
        if (!super.equals(o)) return false;

        GmosNBlueprintImaging that = (GmosNBlueprintImaging) o;

        if (filters != null ? !filters.equals(that.filters) : that.filters != null) return false;
        if (wavelengthRegime != that.wavelengthRegime) return false;
        if (altairConfiguration != that.altairConfiguration) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (wavelengthRegime != null ? wavelengthRegime.hashCode() : 0);
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        result = 31 * result + (altairConfiguration != null ? altairConfiguration.hashCode() : 0);
        return result;
    }

    @Override
    public BlueprintPair toMutable() {
        final GmosNBlueprintChoice choice = new GmosNBlueprintChoice();
        choice.setRegime(getWavelengthRegime());

        final edu.gemini.model.p1.mutable.GmosNBlueprintImaging blueprint = new edu.gemini.model.p1.mutable.GmosNBlueprintImaging();
        choice.setImaging(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.getFilter().addAll(getFilters());
        blueprint.setAltair(getAltairConfiguration().toAltairChoice());

        return new BlueprintPair(choice, blueprint);
    }

    @Override
    public String toString() {
        return "GmosNBlueprintImaging{" +
                "wavelengthRegime=" + wavelengthRegime +
//                ", filters=" + filters +
                "} " + super.toString();
    }

    public GmosNWavelengthRegime getWavelengthRegime() {
        return wavelengthRegime;
    }

    public void setWavelengthRegime(GmosNWavelengthRegime wavelengthRegime) {
        this.wavelengthRegime = wavelengthRegime;
    }

    public List<GmosNFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<GmosNFilter> filters) {
        this.filters = filters;
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        return new GmosSBlueprintImaging(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        Map<String, Set<String>> resources = new HashMap<>();

        resources.put("wavelengthRegime", new HashSet<>());
        resources.put("filter", new HashSet<>());
        resources.put("altairConfiguration", new HashSet<>());

        resources.get("wavelengthRegime").add(wavelengthRegime.name());
        resources.get("altairConfiguration").add(altairConfiguration.value());
        for (GmosNFilter f: filters)
            resources.get("filter").add(f.value());

        return resources;
    }

    @Override
    public boolean isMOS() {
        return false;
    }

    @Override
    public String getDisplayCamera() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayFocalPlaneUnit() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayDisperser() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayFilter() {
        final List<String> values = new ArrayList<String>(getFilters().size());
        for (GmosNFilter f : getFilters()) {
            values.add(f.value());
        }

        return StringUtils.join(values, ",");
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayOther() {
        return getWavelengthRegime().value().value();
    }

    public AltairConfiguration getAltairConfiguration() {
        return altairConfiguration;
    }

    public void setAltairConfiguration(AltairConfiguration altairConfiguration) {
        this.altairConfiguration = altairConfiguration;
    }
}
