package edu.gemini.tac.persistence.phase1.blueprint.flamingos2;

import edu.gemini.model.p1.mutable.Flamingos2BlueprintChoice;
import edu.gemini.model.p1.mutable.Flamingos2Fpu;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gnirs.GnirsBlueprintSpectroscopy;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("Flamingos2BlueprintLongslit")
public class Flamingos2BlueprintLongslit extends Flamingos2BlueprintSpectroscopyBase {
    @Column(name = "fpu")
    @Enumerated(value = EnumType.STRING)
    protected Flamingos2Fpu fpu = Flamingos2Fpu.LONGSLIT_1;

    @SuppressWarnings("unused")
    public Flamingos2BlueprintLongslit() { setInstrument(Instrument.FLAMINGOS2);}

    public Flamingos2BlueprintLongslit(final edu.gemini.model.p1.mutable.Flamingos2BlueprintLongslit mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), mBlueprint.getDisperser(), mBlueprint.getFilter(), new HashSet<>());

        setFpu(mBlueprint.getFpu());
    }

    public Flamingos2BlueprintLongslit(GnirsBlueprintSpectroscopy blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());
    }

    @Override
    public BlueprintPair toMutable() {
        final Flamingos2BlueprintChoice choice = new Flamingos2BlueprintChoice();
        final edu.gemini.model.p1.mutable.Flamingos2BlueprintLongslit blueprint = new edu.gemini.model.p1.mutable.Flamingos2BlueprintLongslit();
        choice.setLongslit(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setDisperser(getDisperser());
        blueprint.getFilter().addAll(filters);
        blueprint.setFpu(getFpu());

        return new BlueprintPair(choice, blueprint);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flamingos2BlueprintLongslit)) return false;
        if (!super.equals(o)) return false;

        Flamingos2BlueprintLongslit that = (Flamingos2BlueprintLongslit) o;

        return fpu == that.fpu;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (fpu != null ? fpu.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Flamingos2BlueprintLongslit{" +
                "fpu=" + fpu +
                "} " + super.toString();
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new GnirsBlueprintSpectroscopy(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        final Map<String, Set<String>> resourceMap = super.getResourcesByCategory();

        resourceMap.put("fpu", new HashSet<>());
        resourceMap.get("fpu").add(fpu.value());

        return resourceMap;
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
        return fpu.value();
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayOther() {
        return DISPLAY_NOT_APPLICABLE;
    }

    public Flamingos2Fpu getFpu() {
        return fpu;
    }

    public void setFpu(Flamingos2Fpu fpu) {
        this.fpu = fpu;
    }
}
