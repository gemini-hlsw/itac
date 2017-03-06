package edu.gemini.tac.persistence.phase1.blueprint.michelle;

import edu.gemini.model.p1.mutable.MichelleBlueprintChoice;
import edu.gemini.model.p1.mutable.MichelleDisperser;
import edu.gemini.model.p1.mutable.MichelleFpu;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.trecs.TrecsBlueprintSpectroscopy;

import javax.persistence.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("MichelleBlueprintSpectroscopy")
public class MichelleBlueprintSpectroscopy extends BlueprintBase {
    @Enumerated(EnumType.STRING)
    @Column(name = "disperser")
    protected MichelleDisperser disperser = MichelleDisperser.ECHELLE;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "fpu")
    protected MichelleFpu fpu = MichelleFpu.MASK_1;

    protected MichelleBlueprintSpectroscopy() { setInstrument(Instrument.MICHELLE); }

    public MichelleBlueprintSpectroscopy(final edu.gemini.model.p1.mutable.MichelleBlueprintSpectroscopy mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), Instrument.MICHELLE);

        setFpu(mBlueprint.getFpu());
        setDisperser(mBlueprint.getDisperser());
    }

    public MichelleBlueprintSpectroscopy(TrecsBlueprintSpectroscopy blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), Instrument.MICHELLE, blueprint.getObservations());
    }

    @Override
    public BlueprintPair toMutable() {
        final MichelleBlueprintChoice choice = new MichelleBlueprintChoice();
        final edu.gemini.model.p1.mutable.MichelleBlueprintSpectroscopy mBlueprint = new edu.gemini.model.p1.mutable.MichelleBlueprintSpectroscopy();
        choice.setSpectroscopy(mBlueprint);

        mBlueprint.setDisperser(getDisperser());
        mBlueprint.setFpu(getFpu());
        mBlueprint.setId(getBlueprintId());
        mBlueprint.setName(getName());

        return new BlueprintPair(choice, mBlueprint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MichelleBlueprintSpectroscopy)) return false;
        if (!super.equals(o)) return false;

        MichelleBlueprintSpectroscopy that = (MichelleBlueprintSpectroscopy) o;

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

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new TrecsBlueprintSpectroscopy(this);
    }

    @Override
    public String toString() {
        return "MichelleBlueprintSpectroscopy{" +
                "disperser=" + disperser +
                ", fpu=" + fpu +
                '}';
    }

    public MichelleDisperser getDisperser() {
        return disperser;
    }

    public void setDisperser(MichelleDisperser disperser) {
        this.disperser = disperser;
    }

    public MichelleFpu getFpu() {
        return fpu;
    }

    public void setFpu(MichelleFpu fpu) {
        this.fpu = fpu;
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        Map<String, Set<String>> resources = new HashMap<String, Set<String>>();

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
        return getFpu().value();
    }

    @Override
    public String getDisplayDisperser() {
        return getDisperser().value();
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
}
