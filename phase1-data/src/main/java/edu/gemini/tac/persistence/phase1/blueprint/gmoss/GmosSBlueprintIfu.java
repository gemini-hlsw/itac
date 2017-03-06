package edu.gemini.tac.persistence.phase1.blueprint.gmoss;

import edu.gemini.model.p1.mutable.GmosSBlueprintChoice;
import edu.gemini.model.p1.mutable.GmosSFpuIfu;
import edu.gemini.model.p1.mutable.GmosSWavelengthRegime;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintIfu;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("GmosSBlueprintIfu")
public class GmosSBlueprintIfu
    extends GmosSBlueprintSpectroscopyBase
{
    @Enumerated(value = EnumType.STRING)
    @Column(name = "fpu")
    protected GmosSFpuIfu fpu = GmosSFpuIfu.IFU_1;

    public GmosSBlueprintIfu() { { setInstrument(Instrument.GMOS_S);}}

    public GmosSBlueprintIfu(edu.gemini.model.p1.mutable.GmosSBlueprintIfu mBlueprint, GmosSWavelengthRegime regime) {
        super(mBlueprint.getId(), mBlueprint.getName(), regime, mBlueprint.getDisperser(), mBlueprint.getFilter());

        setFpu(mBlueprint.getFpu());
        setNodAndShuffle(Boolean.FALSE);
    }

    public GmosSBlueprintIfu(GmosNBlueprintIfu blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosSBlueprintIfu)) return false;
        if (!super.equals(o)) return false;

        GmosSBlueprintIfu that = (GmosSBlueprintIfu) o;

        if (fpu != that.fpu) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (fpu != null ? fpu.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GmosSBlueprintIfu{" +
                "fpu=" + fpu +
                "} " + super.toString();
    }

    @Override
    public BlueprintPair toMutable() {
        final GmosSBlueprintChoice choice = new GmosSBlueprintChoice();
        choice.setRegime(getWavelengthRegime());

        final edu.gemini.model.p1.mutable.GmosSBlueprintIfu blueprint = new edu.gemini.model.p1.mutable.GmosSBlueprintIfu();
        choice.setIfu(blueprint);

        blueprint.setFpu(getFpu());
        blueprint.setFilter(getFilter());
        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setDisperser(getDisperser());

        return new BlueprintPair(choice, blueprint);
    }

    public GmosSFpuIfu getFpu() {
        return fpu;
    }

    public void setFpu(GmosSFpuIfu value) {
        this.fpu = value;
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        return new GmosNBlueprintIfu(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        final Map<String, Set<String>> resources = super.getResourcesByCategory();

        final Set<String> fpus = new HashSet<String>();
        fpus.add(this.getFpu().value());
        resources.put("fpu", fpus);

        return resources;
    }

    @Override
    public String getDisplayFocalPlaneUnit() {
        return getFpu().value();
    }
}
