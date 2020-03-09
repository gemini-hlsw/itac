package edu.gemini.tac.persistence.phase1.blueprint.exchange;

import edu.gemini.model.p1.mutable.KeckInstrument;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;

import javax.persistence.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("KeckBlueprint")
public class KeckBlueprint extends BlueprintBase {
    @Column(name = "exchange_instrument")
    @Enumerated(EnumType.STRING)
    protected KeckInstrument keckInstrument = KeckInstrument.HIRES;

    public KeckBlueprint() { setInstrument(Instrument.KECK);}

    public KeckBlueprint(final edu.gemini.model.p1.mutable.KeckBlueprint mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), Instrument.KECK);

        setKeckInstrument(mBlueprint.getInstrument());
    }

    public KeckInstrument getKeckInstrument() {
        return keckInstrument;
    }

    public void setKeckInstrument(KeckInstrument keckInstrument) {
        this.keckInstrument = keckInstrument;
    }

    @Override
    public BlueprintPair toMutable() {
        final edu.gemini.model.p1.mutable.KeckBlueprint blueprint = new edu.gemini.model.p1.mutable.KeckBlueprint();

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setInstrument(getKeckInstrument());

        return new BlueprintPair(blueprint, blueprint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeckBlueprint)) return false;
        if (!super.equals(o)) return false;

        KeckBlueprint that = (KeckBlueprint) o;

        if (keckInstrument != that.keckInstrument) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (keckInstrument != null ? keckInstrument.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "KeckBlueprint{" +
                "keckInstrument=" + keckInstrument +
                "} " + super.toString();
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        throw new RuntimeException("Switching sites has no meaning for Keck blueprints.");
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        final Map<String, Set<String>> resources = new HashMap<String, Set<String>>();

        resources.put("instrument", new HashSet<String>());
        resources.get("instrument").add(keckInstrument.value());

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
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayOther() {
        return keckInstrument.value();
    }

}
