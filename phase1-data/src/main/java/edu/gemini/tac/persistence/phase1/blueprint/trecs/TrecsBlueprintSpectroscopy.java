package edu.gemini.tac.persistence.phase1.blueprint.trecs;

import edu.gemini.model.p1.mutable.TrecsBlueprintChoice;
import edu.gemini.model.p1.mutable.TrecsDisperser;
import edu.gemini.model.p1.mutable.TrecsFpu;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.michelle.MichelleBlueprintSpectroscopy;

import javax.persistence.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("TrecsBlueprintSpectroscopy")
public class TrecsBlueprintSpectroscopy extends BlueprintBase {
    @Enumerated(EnumType.STRING)
    @Column(name = "disperser")
    protected TrecsDisperser disperser = TrecsDisperser.HIGH_RES;

    @Enumerated(EnumType.STRING)
    @Column(name = "fpu")
    protected TrecsFpu fpu = TrecsFpu.MASK_1;

    @SuppressWarnings("unused")
    public TrecsBlueprintSpectroscopy() { setInstrument(Instrument.TRECS); }

    public TrecsBlueprintSpectroscopy(final edu.gemini.model.p1.mutable.TrecsBlueprintSpectroscopy mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), Instrument.TRECS);

        setDisperser(mBlueprint.getDisperser());
        setFpu(mBlueprint.getFpu());
    }

    public TrecsBlueprintSpectroscopy(MichelleBlueprintSpectroscopy blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), Instrument.TRECS, blueprint.getObservations());
    }

    @Override
    public String toString() {
        return "TrecsBlueprintSpectroscopy{" +
                "disperser=" + disperser +
                ", fpu=" + fpu +
                "} " + super.toString();
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new MichelleBlueprintSpectroscopy(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        final HashMap<String, Set<String>> resources = new HashMap<String, Set<String>>();

        resources.put("disperser", new HashSet<String>());
        resources.put("fpu", new HashSet<String>());

        resources.get("disperser").add(disperser.value());
        resources.get("fpu").add(fpu.value());

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
        return fpu.value();
    }

    @Override
    public String getDisplayDisperser() {
        return disperser.value();
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
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public BlueprintPair toMutable() {
        final TrecsBlueprintChoice choice = new TrecsBlueprintChoice();

        final edu.gemini.model.p1.mutable.TrecsBlueprintSpectroscopy blueprint = new edu.gemini.model.p1.mutable.TrecsBlueprintSpectroscopy();
        choice.setSpectroscopy(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setDisperser(getDisperser());
        blueprint.setFpu(getFpu());

        return new BlueprintPair(choice, blueprint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrecsBlueprintSpectroscopy)) return false;
        if (!super.equals(o)) return false;

        TrecsBlueprintSpectroscopy that = (TrecsBlueprintSpectroscopy) o;

        if (disperser != that.disperser) return false;
        if (fpu != that.fpu) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (disperser != null ? disperser.hashCode() : 0);
        result = 31 * result + (fpu != null ? fpu.hashCode() : 0);
        return result;
    }

    public TrecsDisperser getDisperser() {
        return disperser;
    }

    public void setDisperser(TrecsDisperser disperser) {
        this.disperser = disperser;
    }

    public TrecsFpu getFpu() {
        return fpu;
    }

    public void setFpu(TrecsFpu fpu) {
        this.fpu = fpu;
    }
}
