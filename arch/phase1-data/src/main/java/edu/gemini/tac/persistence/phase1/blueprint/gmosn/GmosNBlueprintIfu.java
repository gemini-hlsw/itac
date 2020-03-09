package edu.gemini.tac.persistence.phase1.blueprint.gmosn;

import edu.gemini.model.p1.mutable.GmosNBlueprintChoice;
import edu.gemini.model.p1.mutable.GmosNFpuIfu;
import edu.gemini.model.p1.mutable.GmosNWavelengthRegime;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintIfu;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("GmosNBlueprintIfu")
public class GmosNBlueprintIfu extends GmosNBlueprintSpectroscopyBase
{
    @Enumerated(value = EnumType.STRING)
    @Column(name = "fpu")
    protected GmosNFpuIfu fpu = GmosNFpuIfu.IFU_1;

    public GmosNBlueprintIfu() { setInstrument(Instrument.GMOS_N);}
    
    public GmosNBlueprintIfu(edu.gemini.model.p1.mutable.GmosNBlueprintIfu mBlueprint, GmosNWavelengthRegime regime) {
        super(mBlueprint.getId(), mBlueprint.getName(), regime, mBlueprint.getDisperser(), mBlueprint.getFilter(), mBlueprint.getAltair());

        setFpu(mBlueprint.getFpu());
    }

    public GmosNBlueprintIfu(GmosSBlueprintIfu blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosNBlueprintIfu)) return false;
        if (!super.equals(o)) return false;

        GmosNBlueprintIfu that = (GmosNBlueprintIfu) o;

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
        return "GmosNBlueprintIfu{" +
                "fpu=" + fpu +
                "} " + super.toString();
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {

        return new GmosSBlueprintIfu(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        final Map<String, Set<String>> resources = super.getResourcesByCategory();

        final Set<String> fpus = new HashSet<>();
        fpus.add(this.getFpu().value());
        resources.put("fpu", fpus);
        resources.put("altairConfiguration", new HashSet<>());
        resources.get("altairConfiguration").add(altairConfiguration.value());

        return resources;
    }

    @Override
    public String getDisplayFocalPlaneUnit() {
        return getFpu().value();
    }

    @Override
    public BlueprintPair toMutable() {
        final GmosNBlueprintChoice choice = new GmosNBlueprintChoice();
        choice.setRegime(getWavelengthRegime());

        final edu.gemini.model.p1.mutable.GmosNBlueprintIfu blueprint = new edu.gemini.model.p1.mutable.GmosNBlueprintIfu();
        choice.setIfu(blueprint);

        blueprint.setFpu(getFpu());
        blueprint.setFilter(getFilter());
        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setDisperser(getDisperser());
        blueprint.setAltair(getAltairConfiguration().toAltairChoice());

        return new BlueprintPair(choice, blueprint);
    }

    public GmosNFpuIfu getFpu() {
        return fpu;
    }

    public void setFpu(GmosNFpuIfu value) {
        this.fpu = value;
    }
}
