package edu.gemini.tac.persistence.phase1.blueprint.gmosn;

import edu.gemini.model.p1.mutable.GmosNBlueprintChoice;
import edu.gemini.model.p1.mutable.GmosNFpuNs;
import edu.gemini.model.p1.mutable.GmosNWavelengthRegime;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintLongSlitNs;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("GmosNBlueprintLongSlitNs")
public class GmosNBlueprintLongSlitNs
        extends GmosNBlueprintSpectroscopyBase
{
    @Enumerated(value = EnumType.STRING)
    @Column(name = "fpu")
    protected GmosNFpuNs fpu = GmosNFpuNs.NS_1;

    protected GmosNBlueprintLongSlitNs() { setInstrument(Instrument.GMOS_N);}

    public GmosNBlueprintLongSlitNs(edu.gemini.model.p1.mutable.GmosNBlueprintLongslitNs mBlueprint, GmosNWavelengthRegime regime) {
        super(mBlueprint.getId(), mBlueprint.getName(), regime, mBlueprint.getDisperser(), mBlueprint.getFilter(), mBlueprint.getAltair());

        setFpu(mBlueprint.getFpu());
    }

    public GmosNBlueprintLongSlitNs(GmosSBlueprintLongSlitNs blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());

        switch (blueprint.getFpu()) {
            case NS_1:
                fpu = GmosNFpuNs.NS_1;
                break;
            case NS_2:
                fpu = GmosNFpuNs.NS_2;
                break;
            case NS_3:
                fpu = GmosNFpuNs.NS_3;
                break;
            case NS_4:
                fpu = GmosNFpuNs.NS_4;
                break;
            case NS_5:
                fpu = GmosNFpuNs.NS_5;
                break;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosNBlueprintLongSlitNs)) return false;
        if (!super.equals(o)) return false;

        GmosNBlueprintLongSlitNs that = (GmosNBlueprintLongSlitNs) o;

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
        return "GmosNBlueprintLongSlitNs{" +
                "fpu=" + fpu +
                "} " + super.toString();
    }

    @Override
    public BlueprintPair toMutable() {
        final GmosNBlueprintChoice choice = new GmosNBlueprintChoice();
        choice.setRegime(getWavelengthRegime());

        final edu.gemini.model.p1.mutable.GmosNBlueprintLongslitNs blueprint = new edu.gemini.model.p1.mutable.GmosNBlueprintLongslitNs();
        choice.setLongslitNs(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setFpu(getFpu());
        blueprint.setFilter(getFilter());
        blueprint.setDisperser(getDisperser());
        blueprint.setAltair(getAltairConfiguration().toAltairChoice());

        return new BlueprintPair(choice, blueprint);
    }

    public GmosNFpuNs getFpu() {
        return fpu;
    }

    public void setFpu(final GmosNFpuNs fpu) {
        this.fpu = fpu;
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        final GmosSBlueprintLongSlitNs blueprint = new GmosSBlueprintLongSlitNs(this);

        return blueprint;
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        final Map<String, Set<String>> resources = super.getResourcesByCategory();

        resources.put("fpu", new HashSet<String>());
        resources.get("fpu").add(fpu.name());
        resources.put("altairConfiguration", new HashSet<String>());
        resources.get("altairConfiguration").add(altairConfiguration.value());

        return resources;
    }

    @Override
    public String getDisplayFocalPlaneUnit() {
        return getFpu().value();
    }
}
