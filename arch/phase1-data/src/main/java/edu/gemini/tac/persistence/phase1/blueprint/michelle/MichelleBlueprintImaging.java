package edu.gemini.tac.persistence.phase1.blueprint.michelle;

import edu.gemini.model.p1.mutable.MichelleBlueprintChoice;
import edu.gemini.model.p1.mutable.MichelleFilter;
import edu.gemini.model.p1.mutable.MichellePolarimetry;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.trecs.TrecsBlueprintImaging;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.util.*;

@Entity
@DiscriminatorValue("MichelleBlueprintImaging")
public class MichelleBlueprintImaging extends BlueprintBase {
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "v2_michelle_imaging_filters",
            joinColumns = @JoinColumn(name = "blueprint_id"))
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    protected Set<MichelleFilter> filters = new HashSet<MichelleFilter>();

    @Enumerated(EnumType.STRING)
    @Column(name = "polarimetry")
    protected MichellePolarimetry polarimetry = MichellePolarimetry.NO;

    public MichelleBlueprintImaging() { setInstrument(Instrument.MICHELLE); }

    public MichelleBlueprintImaging(final edu.gemini.model.p1.mutable.MichelleBlueprintImaging mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), Instrument.MICHELLE);
        getFilters().addAll(mBlueprint.getFilter());
        setPolarimetry(mBlueprint.getPolarimetry());
    }

    public MichelleBlueprintImaging(TrecsBlueprintImaging blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), Instrument.MICHELLE, blueprint.getObservations());
    }

    @Override
    public BlueprintPair toMutable() {
        final MichelleBlueprintChoice choice = new MichelleBlueprintChoice();
        final edu.gemini.model.p1.mutable.MichelleBlueprintImaging mBlueprint = new edu.gemini.model.p1.mutable.MichelleBlueprintImaging();
        choice.setImaging(mBlueprint);

        mBlueprint.getFilter().addAll(filters);
        mBlueprint.setPolarimetry(getPolarimetry());
        mBlueprint.setId(getBlueprintId());
        mBlueprint.setName(getName());

        return new BlueprintPair(choice, mBlueprint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MichelleBlueprintImaging)) return false;
        if (!super.equals(o)) return false;

        MichelleBlueprintImaging that = (MichelleBlueprintImaging) o;

        if (filters != that.filters) return false;
        if (polarimetry != that.polarimetry) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        result = 31 * result + (polarimetry != null ? polarimetry.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MichelleBlueprintImaging{" +
                "filters=" + filters +
                ", polarimetry=" + polarimetry +
                '}';
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new TrecsBlueprintImaging(this);
    }

    public Set<MichelleFilter> getFilters() {
        return filters;
    }

    public void setFilters(Set<MichelleFilter> filters) {
        this.filters = filters;
    }

    public MichellePolarimetry getPolarimetry() {
        return polarimetry;
    }

    public void setPolarimetry(MichellePolarimetry polarimetry) {
        this.polarimetry = polarimetry;
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        Map<String, Set<String>> resources = new HashMap<String, Set<String>>();

        resources.put("filter", new HashSet<String>());
        resources.put("polarimetry", new HashSet<String>());

        for (MichelleFilter f : filters)
            resources.get("filter").add(f.value());
        resources.get("polarimetry").add(polarimetry.value());

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

        for (MichelleFilter f : getFilters())
            values.add(f.value());

        return StringUtils.join(values, ",");
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayOther() {
        return getPolarimetry().value();
    }
}
