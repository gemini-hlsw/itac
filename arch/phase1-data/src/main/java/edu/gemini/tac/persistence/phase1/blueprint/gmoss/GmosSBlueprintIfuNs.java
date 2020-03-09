package edu.gemini.tac.persistence.phase1.blueprint.gmoss;

import edu.gemini.model.p1.mutable.GmosSBlueprintChoice;
import edu.gemini.model.p1.mutable.GmosSFpuIfuNs;
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
@DiscriminatorValue("GmosSBlueprintIfuNs")
public class GmosSBlueprintIfuNs
    extends GmosSBlueprintSpectroscopyBase
{
    @Enumerated(value = EnumType.STRING)
    @Column(name = "fpu")
    protected GmosSFpuIfuNs fpu = GmosSFpuIfuNs.IFU_N;

    public GmosSBlueprintIfuNs() { { setInstrument(Instrument.GMOS_S);}}

    public GmosSBlueprintIfuNs(edu.gemini.model.p1.mutable.GmosSBlueprintIfuNs mBlueprint, GmosSWavelengthRegime regime) {
        super(mBlueprint.getId(), mBlueprint.getName(), regime, mBlueprint.getDisperser(), mBlueprint.getFilter());
        setFpu(mBlueprint.getFpu());
        setNodAndShuffle(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosSBlueprintIfuNs)) return false;
        if (!super.equals(o)) return false;

        GmosSBlueprintIfuNs that = (GmosSBlueprintIfuNs) o;

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

        final edu.gemini.model.p1.mutable.GmosSBlueprintIfuNs blueprint = new edu.gemini.model.p1.mutable.GmosSBlueprintIfuNs();
        choice.setIfuNs(blueprint);
        blueprint.setFpu(getFpu());
        blueprint.setFilter(getFilter());
        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setDisperser(getDisperser());

        return new BlueprintPair(choice, blueprint);
    }

    public GmosSFpuIfuNs getFpu() {
        return fpu;
    }

    public void setFpu(GmosSFpuIfuNs value) {
        this.fpu = value;
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        return new GmosNBlueprintIfu();
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
