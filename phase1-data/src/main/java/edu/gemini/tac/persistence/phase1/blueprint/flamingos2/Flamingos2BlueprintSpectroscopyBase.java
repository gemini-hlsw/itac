package edu.gemini.tac.persistence.phase1.blueprint.flamingos2;

import edu.gemini.model.p1.mutable.Flamingos2Disperser;
import edu.gemini.model.p1.mutable.Flamingos2Filter;
import edu.gemini.tac.persistence.phase1.Observation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
abstract public class Flamingos2BlueprintSpectroscopyBase extends Flamingos2BlueprintBase {
    @Column(name = "disperser")
    @Enumerated(value = EnumType.STRING)
    protected Flamingos2Disperser disperser = Flamingos2Disperser.R1200HK;

    public Flamingos2BlueprintSpectroscopyBase() {}

    public Flamingos2BlueprintSpectroscopyBase(final String id, final String name, final Flamingos2Disperser disperser, final List<Flamingos2Filter> filters, final Set<Observation> observations) {
        super(id, name, observations, filters);
        this.disperser = disperser;
    }

    public Flamingos2BlueprintSpectroscopyBase(final String id, final String name) {
        super(id, name);
    }

    public Flamingos2BlueprintSpectroscopyBase(final String id, final String name, final Set<Observation> observations){
        super(id, name, observations);
    }

    @Override
    public String getDisplayDisperser() { return disperser.value(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flamingos2BlueprintSpectroscopyBase)) return false;
        if (!super.equals(o)) return false;

        Flamingos2BlueprintSpectroscopyBase that = (Flamingos2BlueprintSpectroscopyBase) o;

        return disperser == that.disperser;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (disperser != null ? disperser.hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        return "Flamingos2BlueprintSpectroscopyBase{" +
                "disperser=" + disperser +
                "} " + super.toString();
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        Map<String, Set<String>> resources = super.getResourcesByCategory();

        resources.put("disperser", new HashSet<>());
        resources.get("disperser").add(disperser.value());

        return resources;
    }

    public Flamingos2Disperser getDisperser() {
        return disperser;
    }

    public void setDisperser(Flamingos2Disperser disperser) {
        this.disperser = disperser;
    }
}
