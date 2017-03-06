package edu.gemini.tac.persistence.phase1.blueprint.gmoss;

import edu.gemini.model.p1.mutable.GmosSBlueprintChoice;
import edu.gemini.model.p1.mutable.GmosSFilter;
import edu.gemini.model.p1.mutable.GmosSWavelengthRegime;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintImaging;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@DiscriminatorValue("GmosSBlueprintImaging")
public class GmosSBlueprintImaging
    extends BlueprintBase {

    @Enumerated(EnumType.STRING)
    @Column(name = "wavelength_regime")
    protected GmosSWavelengthRegime wavelengthRegime = GmosSWavelengthRegime.OPTICAL;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "v2_gmoss_filters",
            joinColumns = @JoinColumn(name = "blueprint_id"))
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    protected List<GmosSFilter> filters = new ArrayList<GmosSFilter>();

    public GmosSBlueprintImaging() { { setInstrument(Instrument.GMOS_S);}}

    public GmosSBlueprintImaging(edu.gemini.model.p1.mutable.GmosSBlueprintImaging mBlueprint, GmosSWavelengthRegime regime) {
        super(mBlueprint.getId(), mBlueprint.getName(), Instrument.GMOS_S);

        getFilters().addAll(mBlueprint.getFilter());
        setWavelengthRegime(regime);
    }

    public GmosSBlueprintImaging(GmosNBlueprintImaging blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), Instrument.GMOS_S, blueprint.getObservations());
    }

    public GmosSBlueprintImaging(GmosSBlueprintImaging blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), Instrument.GMOS_S);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosSBlueprintImaging)) return false;
        if (!super.equals(o)) return false;

        GmosSBlueprintImaging that = (GmosSBlueprintImaging) o;

        if (filters != null ? !filters.equals(that.filters) : that.filters != null) return false;
        if (wavelengthRegime != that.wavelengthRegime) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (wavelengthRegime != null ? wavelengthRegime.hashCode() : 0);
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        return result;
    }

    @Override
    public BlueprintPair toMutable() {
        final GmosSBlueprintChoice choice = new GmosSBlueprintChoice();
        choice.setRegime(getWavelengthRegime());

        final edu.gemini.model.p1.mutable.GmosSBlueprintImaging blueprint = new edu.gemini.model.p1.mutable.GmosSBlueprintImaging();
        choice.setImaging(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.getFilter().addAll(getFilters());

        return new BlueprintPair(choice, blueprint);
    }

    @Override
    public String toString() {
        return "GmosSBlueprintImaging{" +
                "wavelengthRegime=" + wavelengthRegime +
//                ", filters=" + filters +
                "} " + super.toString();
    }

    public GmosSWavelengthRegime getWavelengthRegime() {
        return wavelengthRegime;
    }

    public void setWavelengthRegime(GmosSWavelengthRegime wavelengthRegime) {
        this.wavelengthRegime = wavelengthRegime;
    }

    public List<GmosSFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<GmosSFilter> filters) {
        this.filters = filters;
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        return new GmosNBlueprintImaging(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        Map<String, Set<String>> resources = new HashMap<>();

        final Set<String> filters = new HashSet<>();
        for(GmosSFilter filter : this.getFilters()){
            filters.add(filter.value());
        }
        resources.put("filter", filters);

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
        final List<String> values = new ArrayList<>(getFilters().size());
        values.addAll(getFilters().stream().map(GmosSFilter::value).collect(Collectors.toList()));

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
}

