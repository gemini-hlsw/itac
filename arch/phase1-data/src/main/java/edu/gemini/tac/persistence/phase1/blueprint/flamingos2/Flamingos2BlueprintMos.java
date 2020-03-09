package edu.gemini.tac.persistence.phase1.blueprint.flamingos2;

import edu.gemini.model.p1.mutable.Flamingos2BlueprintChoice;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gnirs.GnirsBlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.nifs.NifsBlueprint;
import edu.gemini.tac.persistence.phase1.blueprint.nifs.NifsBlueprintAo;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("Flamingos2BlueprintMos")
public class Flamingos2BlueprintMos extends Flamingos2BlueprintSpectroscopyBase {
    @Column(name = "preimaging")
    protected Boolean preimaging = Boolean.FALSE;

    @SuppressWarnings("unused")
    public Flamingos2BlueprintMos() { setInstrument(Instrument.FLAMINGOS2);}

    public Flamingos2BlueprintMos(final edu.gemini.model.p1.mutable.Flamingos2BlueprintMos mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), mBlueprint.getDisperser(), mBlueprint.getFilter(), new HashSet<>());

        setPreimaging(mBlueprint.isPreimaging());
    }

    public Flamingos2BlueprintMos(NifsBlueprint blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());
    }

    public Flamingos2BlueprintMos(NifsBlueprintAo blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());
    }

    @Override
    public BlueprintPair toMutable() {
        final Flamingos2BlueprintChoice choice = new Flamingos2BlueprintChoice();
        final edu.gemini.model.p1.mutable.Flamingos2BlueprintMos blueprint = new edu.gemini.model.p1.mutable.Flamingos2BlueprintMos();
        choice.setMos(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setDisperser(getDisperser());
        blueprint.getFilter().addAll(filters);
        blueprint.setPreimaging(preimaging);

        return new BlueprintPair(choice, blueprint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flamingos2BlueprintMos)) return false;
        if (!super.equals(o)) return false;

        Flamingos2BlueprintMos that = (Flamingos2BlueprintMos) o;

        return !(preimaging != null ? !preimaging.equals(that.preimaging) : that.preimaging != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (preimaging != null ? preimaging.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Flamingos2BlueprintMos{" +
                "preimaging=" + preimaging +
                "} " + super.toString();
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new GnirsBlueprintImaging(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        final Map<String, Set<String>> resourceMap = super.getResourcesByCategory();

        resourceMap.put("preimaging", new HashSet<>());
        resourceMap.get("preimaging").add(preimaging ? "preimaging" : "");

        return resourceMap;
    }

    @Override
    public boolean isMOS() {
        return true;
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
    public String getDisplayAdaptiveOptics() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayOther() {
        return (preimaging) ? "preimaging" : "not preimaging";
    }

    public Boolean getPreimaging() {
        return preimaging;
    }

    public void setPreimaging(Boolean preimaging) {
        this.preimaging = preimaging;
    }
}
