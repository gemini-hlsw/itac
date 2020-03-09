package edu.gemini.tac.persistence.phase1.blueprint.niri;

import edu.gemini.model.p1.mutable.*;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.altair.AltairConfiguration;
import edu.gemini.tac.persistence.phase1.blueprint.flamingos2.Flamingos2BlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.gsaoi.*;
import edu.gemini.tac.persistence.phase1.blueprint.gsaoi.GsaoiBlueprint;
import edu.gemini.tac.persistence.phase1.blueprint.nici.NiciBlueprintCoronagraphic;
import edu.gemini.tac.persistence.phase1.blueprint.nici.NiciBlueprintStandard;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.util.*;

@Entity
@DiscriminatorValue("NiriBlueprint")
public class NiriBlueprint extends BlueprintBase {
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "v2_niri_filters",
            joinColumns = @JoinColumn(name = "blueprint_id"))
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    protected List<NiriFilter> filters = new ArrayList<NiriFilter>();

    @Column(name = "camera")
    @Enumerated(EnumType.STRING)
    protected NiriCamera camera = NiriCamera.F6;

    @Enumerated(EnumType.STRING)
    @Column(name = "altair_configuration")
    protected AltairConfiguration altairConfiguration = AltairConfiguration.NONE;

    public NiriBlueprint() {
        setInstrument(Instrument.NIRI);
    }

    public NiriBlueprint(final edu.gemini.model.p1.mutable.NiriBlueprint mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), Instrument.NIRI);
        setAltairConfiguration(AltairConfiguration.fromAltairChoice(mBlueprint.getAltair()));
        setFilters(mBlueprint.getFilter());
        setCamera(mBlueprint.getCamera());
    }

    public NiriBlueprint(final Flamingos2BlueprintImaging blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), Instrument.NIRI, blueprint.getObservations());
    }

    public NiriBlueprint(NiciBlueprintStandard blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), Instrument.NIRI, blueprint.getObservations());
    }

    public NiriBlueprint(NiciBlueprintCoronagraphic blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), Instrument.NIRI, blueprint.getObservations());
    }

    public NiriBlueprint(GsaoiBlueprint blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), Instrument.NIRI, blueprint.getObservations());
    }

    @Override
    public BlueprintPair toMutable() {
        final NiriBlueprintChoice choice = new NiriBlueprintChoice();
        final edu.gemini.model.p1.mutable.NiriBlueprint blueprint = new edu.gemini.model.p1.mutable.NiriBlueprint();
        choice.setNiri(blueprint);
        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setAltair(getAltairConfiguration().toAltairChoice());
        blueprint.getFilter().addAll(getFilters());
        blueprint.setCamera(getCamera());

        return new BlueprintPair(choice, blueprint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NiriBlueprint)) return false;
        if (!super.equals(o)) return false;

        NiriBlueprint that = (NiriBlueprint) o;

        if (altairConfiguration != that.altairConfiguration) return false;
        if (camera != that.camera) return false;
        if (filters != null ? !filters.equals(that.filters) : that.filters != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        result = 31 * result + (camera != null ? camera.hashCode() : 0);
        result = 31 * result + (altairConfiguration != null ? altairConfiguration.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NiriBlueprint{" +
                "filters=" + filters +
                ", camera=" + camera +
                ", altairConfiguration=" + altairConfiguration +
                "} " + super.toString();
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new Flamingos2BlueprintImaging(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        final Map<String, Set<String>> resources = new HashMap<String, Set<String>>();

        resources.put("filter", new HashSet<String>());
        resources.put("camera", new HashSet<String>());
        resources.put("altairConfiguration", new HashSet<String>());

        for (NiriFilter filter : filters)
            resources.get("filter").add(filter.value());
        resources.get("camera").add(camera.value());
        resources.get("altairConfiguration").add(altairConfiguration.value());
        
        return resources;
    }

    public List<NiriFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<NiriFilter> filters) {
        this.filters = filters;
    }

    public NiriCamera getCamera() {
        return camera;
    }

    public void setCamera(NiriCamera camera) {
        this.camera = camera;
    }

    public AltairConfiguration getAltairConfiguration() {
        return altairConfiguration;
    }

    public void setAltairConfiguration(AltairConfiguration altairConfiguration) {
        this.altairConfiguration = altairConfiguration;
    }

    @Override
    public String getDisplayCamera() {
        return camera.toString();
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
        return altairConfiguration.getDisplayValue();
    }

    @Override
    public String getDisplayOther() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public boolean hasLgs(){
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

    @Override
    public boolean isMOS() {
        return false;
    }
}
