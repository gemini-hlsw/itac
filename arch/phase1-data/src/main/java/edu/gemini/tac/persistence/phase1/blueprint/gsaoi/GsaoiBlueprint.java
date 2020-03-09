package edu.gemini.tac.persistence.phase1.blueprint.gsaoi;

import edu.gemini.model.p1.mutable.GsaoiBlueprintChoice;
import edu.gemini.model.p1.mutable.GsaoiFilter;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.niri.NiriBlueprint;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.util.*;

@Entity
@DiscriminatorValue("GsaoiBlueprint")
public class GsaoiBlueprint extends BlueprintBase {
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "v2_gsaoi_filters",
            joinColumns = @JoinColumn(name = "blueprint_id"))
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    protected List<GsaoiFilter> filters = new ArrayList<GsaoiFilter>();

    public GsaoiBlueprint() {
        setInstrument(Instrument.GSAOI);
    }

    public GsaoiBlueprint(final edu.gemini.model.p1.mutable.GsaoiBlueprint mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), Instrument.GSAOI);
        setFilters(mBlueprint.getFilter());
    }

    @Override
    public BlueprintPair toMutable() {
        final GsaoiBlueprintChoice choice = new GsaoiBlueprintChoice();
        final edu.gemini.model.p1.mutable.GsaoiBlueprint blueprint = new edu.gemini.model.p1.mutable.GsaoiBlueprint();
        choice.setGsaoi(blueprint);
        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.getFilter().addAll(getFilters());

        return new BlueprintPair(choice, blueprint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GsaoiBlueprint)) return false;
        if (!super.equals(o)) return false;

        GsaoiBlueprint that = (GsaoiBlueprint) o;

        if (filters != null ? !filters.equals(that.filters) : that.filters != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        return result;
    }

    public List<GsaoiFilter> getFilters() {

        return filters;
    }

    public void setFilters(List<GsaoiFilter> filters) {
        this.filters = filters;
    }

    @Override
    public String toString() {
        return "GsaoiBlueprint{" +
                "filters=" + filters +
                '}';
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new NiriBlueprint(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        final Map<String, Set<String>> resources = new HashMap<String, Set<String>>();

        resources.put("filter", new HashSet<String>());
        resources.put("camera", new HashSet<String>());
        resources.put("altairConfiguration", new HashSet<String>());

        for (GsaoiFilter filter : filters)
            resources.get("filter").add(filter.value());

        return resources;
    }

    @Override
    public boolean hasMcao() {
        return true;
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
        return StringUtils.join(filters, ",");
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        return "GSAOI";
    }

    @Override
    public String getDisplayOther() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public boolean hasLgs(){
        return true;
    }
}
