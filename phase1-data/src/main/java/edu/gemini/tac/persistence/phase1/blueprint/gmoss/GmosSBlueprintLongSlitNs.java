package edu.gemini.tac.persistence.phase1.blueprint.gmoss;

import edu.gemini.model.p1.mutable.GmosSBlueprintChoice;
import edu.gemini.model.p1.mutable.GmosSFpuNs;
import edu.gemini.model.p1.mutable.GmosSWavelengthRegime;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintLongSlitNs;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("GmosSBlueprintLongSlitNs")
public class GmosSBlueprintLongSlitNs
        extends GmosSBlueprintSpectroscopyBase
{
    @Enumerated(value = EnumType.STRING)
    @Column(name = "fpu")
    protected GmosSFpuNs fpu = GmosSFpuNs.NS_1;

    public GmosSBlueprintLongSlitNs() { setInstrument(Instrument.GMOS_S); }

    public GmosSBlueprintLongSlitNs(edu.gemini.model.p1.mutable.GmosSBlueprintLongslitNs mBlueprint, GmosSWavelengthRegime regime) {
        super(mBlueprint.getId(), mBlueprint.getName(), regime, mBlueprint.getDisperser(), mBlueprint.getFilter());

        setFpu(mBlueprint.getFpu());
        setNodAndShuffle(Boolean.TRUE);
    }

    public GmosSBlueprintLongSlitNs(GmosNBlueprintLongSlitNs blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());

        switch (blueprint.getFpu()) {
            case NS_1:
                fpu = GmosSFpuNs.NS_1;
                break;
            case NS_2:
                fpu = GmosSFpuNs.NS_2;
                break;
            case NS_3:
                fpu = GmosSFpuNs.NS_3;
                break;
            case NS_4:
                fpu = GmosSFpuNs.NS_4;
                break;
            case NS_5:
                fpu = GmosSFpuNs.NS_5;
                break;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosSBlueprintLongSlitNs)) return false;
        if (!super.equals(o)) return false;

        GmosSBlueprintLongSlitNs that = (GmosSBlueprintLongSlitNs) o;

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
        return "GmosSBlueprintLongSlitNs{" +
                "fpu=" + fpu +
                "} " + super.toString();
    }

    @Override
    public BlueprintPair toMutable() {
        final GmosSBlueprintChoice choice = new GmosSBlueprintChoice();
        choice.setRegime(getWavelengthRegime());

        final edu.gemini.model.p1.mutable.GmosSBlueprintLongslitNs blueprint = new edu.gemini.model.p1.mutable.GmosSBlueprintLongslitNs();
        choice.setLongslitNs(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setFpu(getFpu());
        blueprint.setFilter(getFilter());
        blueprint.setDisperser(getDisperser());

        return new BlueprintPair(choice, blueprint);
    }

    public GmosSFpuNs getFpu() {
        return fpu;
    }

    public void setFpu(final GmosSFpuNs fpu) {
        this.fpu = fpu;
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        return new GmosNBlueprintLongSlitNs(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        final Map<String, Set<String>> resources = super.getResourcesByCategory();

        resources.put("fpu", new HashSet<String>());
        resources.get("fpu").add(fpu.name());

        return resources;
    }

    @Override
    public String getDisplayFocalPlaneUnit() {
        return getFpu().value();
    }
}
