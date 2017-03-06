package edu.gemini.tac.persistence.phase1.blueprint.flamingos2;

import edu.gemini.model.p1.mutable.Flamingos2Filter;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;
import java.util.*;

@Entity
abstract public class Flamingos2BlueprintBase extends BlueprintBase {
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "v2_flamingos2_filters",
            joinColumns = @JoinColumn(name = "blueprint_id"))
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    protected final List<Flamingos2Filter> filters = new ArrayList<>();

    public Flamingos2BlueprintBase() {}

    public Flamingos2BlueprintBase(final String id, final String name) {
        super(id, name, Instrument.FLAMINGOS2);
    }

    public Flamingos2BlueprintBase(final String id, final String name, Set<Observation> observations){
        super(id, name, Instrument.FLAMINGOS2, observations);
    }

    public Flamingos2BlueprintBase(final String id, final String name, List<Flamingos2Filter> filter) {
        super(id, name, Instrument.FLAMINGOS2);

        this.filters.addAll(filter);
    }

    public Flamingos2BlueprintBase(final String id, final String name, Set<Observation> observations, List<Flamingos2Filter> filters){
        super(id, name, Instrument.FLAMINGOS2, observations);
        this.filters.addAll(filters);
    }

    @Override
    public String toString() {
        return "Flamingos2BlueprintBase{" +
                "filters=" + filters +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flamingos2BlueprintBase)) return false;
        if (!super.equals(o)) return false;

        Flamingos2BlueprintBase that = (Flamingos2BlueprintBase) o;

        return !(filters != null ? !filters.equals(that.filters) : that.filters != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        return result;
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        Map<String, Set<String>> resources = new HashMap<>();

        resources.put("filter", new HashSet<>());
        for (Flamingos2Filter f: filters)
            resources.get("filter").add(f.value());

        return resources;
    }

    @Override
    public String getDisplayFilter() {
        return StringUtils.join(filters, ",");
    }
}
