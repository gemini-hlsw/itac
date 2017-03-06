package edu.gemini.tac.persistence.phase1.blueprint.trecs;

import edu.gemini.model.p1.mutable.TrecsBlueprintChoice;
import edu.gemini.model.p1.mutable.TrecsFilter;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.michelle.MichelleBlueprintImaging;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.util.*;

@Entity
@DiscriminatorValue("TrecsBlueprintImaging")
public class TrecsBlueprintImaging extends BlueprintBase {
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "v2_trecs_filters",
            joinColumns = @JoinColumn(name = "blueprint_id"))
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    protected List<TrecsFilter> filters = new ArrayList<TrecsFilter>();

    @SuppressWarnings("unused")
    public TrecsBlueprintImaging() { setInstrument(Instrument.TRECS); }

    public TrecsBlueprintImaging(edu.gemini.model.p1.mutable.TrecsBlueprintImaging mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), Instrument.TRECS);
        setFilters(mBlueprint.getFilter());
    }

    public TrecsBlueprintImaging(MichelleBlueprintImaging blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), Instrument.TRECS, blueprint.getObservations());
    }

    @Override
    public String toString() {
        return "TrecsBlueprintImaging{" +
                "filters=" + filters +
                "} " + super.toString();
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new MichelleBlueprintImaging(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        final HashMap<String, Set<String>> resources = new HashMap<String, Set<String>>();

        resources.put("filter", new HashSet<String>());

        for (TrecsFilter f : filters)
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
        return StringUtils.join(filters, ",");
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayOther() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public BlueprintPair toMutable() {
        final TrecsBlueprintChoice choice = new TrecsBlueprintChoice();

        final edu.gemini.model.p1.mutable.TrecsBlueprintImaging blueprint = new edu.gemini.model.p1.mutable.TrecsBlueprintImaging();
        choice.setImaging(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.getFilter().addAll(filters);

        return new BlueprintPair(choice, blueprint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrecsBlueprintImaging)) return false;
        if (!super.equals(o)) return false;

        TrecsBlueprintImaging that = (TrecsBlueprintImaging) o;

        if (filters != null ? !filters.equals(that.filters) : that.filters != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        return result;
    }

    public List<TrecsFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<TrecsFilter> filters) {
        this.filters = filters;
    }
}
