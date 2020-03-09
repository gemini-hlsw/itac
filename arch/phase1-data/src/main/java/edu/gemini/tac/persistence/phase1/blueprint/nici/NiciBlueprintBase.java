package edu.gemini.tac.persistence.phase1.blueprint.nici;

import edu.gemini.model.p1.mutable.NiciBlueFilter;
import edu.gemini.model.p1.mutable.NiciDichroic;
import edu.gemini.model.p1.mutable.NiciRedFilter;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.util.*;

@Entity
abstract public class NiciBlueprintBase extends BlueprintBase {
    @Column(name = "dichroic")
    @Enumerated(value = EnumType.STRING)
    protected NiciDichroic dichroic = NiciDichroic.CH4_H_DICHROIC;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "v2_nici_red_filters",
            joinColumns = @JoinColumn(name = "blueprint_id"))
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    protected List<NiciRedFilter> redFilters = new ArrayList<NiciRedFilter>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "v2_nici_blue_filters",
            joinColumns = @JoinColumn(name = "blueprint_id"))
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    protected List<NiciBlueFilter> blueFilters = new ArrayList<NiciBlueFilter>();

    public NiciBlueprintBase() {}

    public NiciBlueprintBase(String id, String name, NiciDichroic dichroic, List<NiciRedFilter> redFilters, List<NiciBlueFilter> blueFilters, Set<Observation> observations) {
        super(id, name, Instrument.NICI, observations);

        this.dichroic = dichroic;
        this.redFilters = redFilters;
        this.blueFilters = blueFilters;
    }

    public NiciBlueprintBase(String id, String name, Set<Observation> observations) {
        super(id, name, Instrument.NICI, observations);
    }

    @Override
    public String toString() {
        return "NiciBlueprintBase{" +
                "dichroic=" + dichroic +
                ", redFilters=" + redFilters +
                ", blueFilters=" + blueFilters +
                "} " + super.toString();
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        Map<String, Set<String>> resources = new HashMap<String, Set<String>>();

        resources.put("dichroic", new HashSet<String>());
        resources.put("filter", new HashSet<String>());

        resources.get("dichroic").add(dichroic.value());
        for (NiciRedFilter filter : redFilters)
            resources.get("filter").add(filter.value());
        for (NiciBlueFilter filter : blueFilters)
            resources.get("filter").add(filter.value());

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
        return "R:" + StringUtils.join(redFilters, ",") + ", B:" + StringUtils.join(blueFilters, ",");
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NiciBlueprintBase)) return false;
        if (!super.equals(o)) return false;

        NiciBlueprintBase that = (NiciBlueprintBase) o;

        if (blueFilters != that.blueFilters) return false;
        if (dichroic != that.dichroic) return false;
        if (redFilters != that.redFilters) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (dichroic != null ? dichroic.hashCode() : 0);
        result = 31 * result + (redFilters != null ? redFilters.hashCode() : 0);
        result = 31 * result + (blueFilters != null ? blueFilters.hashCode() : 0);
        return result;
    }

    public NiciDichroic getDichroic() {
        return dichroic;
    }

    public void setDichroic(NiciDichroic dichroic) {
        this.dichroic = dichroic;
    }

    public List<NiciRedFilter> getRedFilters() {
        return redFilters;
    }

    public void setRedFilters(List<NiciRedFilter> redFilters) {
        this.redFilters = redFilters;
    }

    public List<NiciBlueFilter> getBlueFilters() {
        return blueFilters;
    }

    public void setBlueFilters(List<NiciBlueFilter> blueFilters) {
        this.blueFilters = blueFilters;
    }
}
