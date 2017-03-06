package edu.gemini.tac.persistence.phase1.blueprint.gmoss;

import edu.gemini.model.p1.mutable.GmosSBlueprintChoice;
import edu.gemini.model.p1.mutable.GmosSFpu;
import edu.gemini.model.p1.mutable.GmosSWavelengthRegime;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintLongSlit;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("GmosSBlueprintLongSlit")
public class GmosSBlueprintLongSlit
        extends GmosSBlueprintSpectroscopyBase
{
    @Enumerated(value = EnumType.STRING)
    @Column(name = "fpu")
    protected GmosSFpu fpu = GmosSFpu.LONGSLIT_1;

    public GmosSBlueprintLongSlit() { { setInstrument(Instrument.GMOS_S);}}

    public GmosSBlueprintLongSlit(edu.gemini.model.p1.mutable.GmosSBlueprintLongslit mBlueprint, GmosSWavelengthRegime regime) {
        super(mBlueprint.getId(), mBlueprint.getName(), regime, mBlueprint.getDisperser(), mBlueprint.getFilter());

        setFpu(mBlueprint.getFpu());
        setNodAndShuffle(Boolean.FALSE);
    }

    public GmosSBlueprintLongSlit(GmosNBlueprintLongSlit blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosSBlueprintLongSlit)) return false;
        if (!super.equals(o)) return false;

        GmosSBlueprintLongSlit that = (GmosSBlueprintLongSlit) o;

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
        return "GmosSBlueprintLongSlit{" +
                "fpu=" + fpu +
                "} " + super.toString();
    }

    @Override
    public BlueprintPair toMutable() {
        final GmosSBlueprintChoice choice = new GmosSBlueprintChoice();
        choice.setRegime(getWavelengthRegime());

        final edu.gemini.model.p1.mutable.GmosSBlueprintLongslit blueprint = new edu.gemini.model.p1.mutable.GmosSBlueprintLongslit();
        choice.setLongslit(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setFpu(getFpu());
        blueprint.setFilter(getFilter());
        blueprint.setDisperser(getDisperser());

        return new BlueprintPair(choice, blueprint);
    }

    public GmosSFpu getFpu() {
        return fpu;
    }

    public void setFpu(GmosSFpu fpu) {
        this.fpu = fpu;
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        return new GmosNBlueprintLongSlit(this);
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
