package edu.gemini.tac.persistence.phase1.blueprint.nifs;

import edu.gemini.model.p1.mutable.NifsDisperser;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
abstract public class NifsBlueprintBase extends BlueprintBase {
    @Enumerated(EnumType.STRING)
    @Column(name = "disperser")
    protected NifsDisperser disperser;

    protected NifsBlueprintBase() {}

    public NifsBlueprintBase(final String id, final String name, final NifsDisperser disperser, final Set<Observation> observations) {
        super(id, name, Instrument.NIFS, observations);

        setDisperser(disperser);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NifsBlueprintBase)) return false;
        if (!super.equals(o)) return false;

        NifsBlueprintBase that = (NifsBlueprintBase) o;

        if (disperser != that.disperser) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (disperser != null ? disperser.hashCode() : 0);
        return result;
    }

    @Override
    public boolean isMOS() {
        return false;
    }

    @Override
    public String toString() {
        return "NifsBlueprintBase{" +
                "disperser=" + disperser +
                '}';
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        final HashMap<String, Set<String>> resources = new HashMap<String, Set<String>>();

        resources.put("disperser", new HashSet<String>());

        resources.get("disperser").add(disperser.value());

        return resources;
    }

    public NifsDisperser getDisperser() {
        return disperser;
    }

    public void setDisperser(NifsDisperser disperser) {
        this.disperser = disperser;
    }

    @Override
    public String getDisplayDisperser() {
        return getDisperser().value();
    }
}
