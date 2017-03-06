package edu.gemini.tac.persistence.phase1.blueprint.gnirs;

import edu.gemini.model.p1.mutable.GnirsBlueprintChoice;
import edu.gemini.model.p1.mutable.GnirsFilter;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.flamingos2.Flamingos2BlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.flamingos2.Flamingos2BlueprintMos;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("GnirsBlueprintImaging")
public class GnirsBlueprintImaging extends GnirsBlueprintBase {

    @Enumerated(EnumType.STRING)
    @Column(name = "filter")
    protected GnirsFilter filter = GnirsFilter.H2;

    public GnirsBlueprintImaging() { setInstrument(Instrument.GNIRS); }

    public GnirsBlueprintImaging(final edu.gemini.model.p1.mutable.GnirsBlueprintImaging mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), mBlueprint.getAltair(), mBlueprint.getPixelScale(), new HashSet<Observation>());
        setFilter(mBlueprint.getFilter());
    }

    public GnirsBlueprintImaging(final Flamingos2BlueprintMos blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());
    }

    @Override
    public BlueprintPair toMutable() {
        final GnirsBlueprintChoice choice = new GnirsBlueprintChoice();
        final edu.gemini.model.p1.mutable.GnirsBlueprintImaging mBlueprint = new edu.gemini.model.p1.mutable.GnirsBlueprintImaging();
        choice.setImaging(mBlueprint);

        mBlueprint.setAltair(getAltairConfiguration().toAltairChoice());
        mBlueprint.setFilter(getFilter());
        mBlueprint.setId(getBlueprintId());
        mBlueprint.setName(getName());
        mBlueprint.setPixelScale(getPixelScale());

        return new BlueprintPair(choice, mBlueprint);
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        return new Flamingos2BlueprintImaging(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GnirsBlueprintImaging)) return false;
        if (!super.equals(o)) return false;

        GnirsBlueprintImaging that = (GnirsBlueprintImaging) o;

        if (filter != that.filter) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GnirsBlueprintImaging{" +
                "filter=" + filter +
                '}';
    }

    public GnirsFilter getFilter() {
        return filter;
    }

    public void setFilter(GnirsFilter filter) {
        this.filter = filter;
    }


    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        Map<String, Set<String>> resources = super.getResourcesByCategory();

        resources.put("filter", new HashSet<String>());
        resources.get("filter").add(filter.value());

        return resources;
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
        return getFilter().value();
    }

    @Override
    public String getDisplayOther() {
        return DISPLAY_NOT_APPLICABLE;
    }
}
