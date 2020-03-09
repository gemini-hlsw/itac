package edu.gemini.tac.persistence.phase1.blueprint.gmosn;

import edu.gemini.model.p1.mutable.GmosNBlueprintChoice;
import edu.gemini.model.p1.mutable.GmosNFpu;
import edu.gemini.model.p1.mutable.GmosNWavelengthRegime;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintLongSlit;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("GmosNBlueprintLongSlit")
public class GmosNBlueprintLongSlit
        extends GmosNBlueprintSpectroscopyBase
{
    @Enumerated(value = EnumType.STRING)
    @Column(name = "fpu")
    protected GmosNFpu fpu = GmosNFpu.LONGSLIT_2;

    protected GmosNBlueprintLongSlit() { setInstrument(Instrument.GMOS_N);}

    public GmosNBlueprintLongSlit(final edu.gemini.model.p1.mutable.GmosNBlueprintLongslit mBlueprint, final GmosNWavelengthRegime regime) {
        super(mBlueprint.getId(), mBlueprint.getName(), regime, mBlueprint.getDisperser(), mBlueprint.getFilter(), mBlueprint.getAltair());

        setFpu(mBlueprint.getFpu());
    }

    public GmosNBlueprintLongSlit(GmosSBlueprintLongSlit blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());

        switch (blueprint.getFpu()) {
            case LONGSLIT_2:
                fpu = GmosNFpu.LONGSLIT_2;
                break;
            case LONGSLIT_3:
                fpu = GmosNFpu.LONGSLIT_3;
                break;
            case LONGSLIT_4:
                fpu = GmosNFpu.LONGSLIT_4;
                break;
            case LONGSLIT_5:
                fpu = GmosNFpu.LONGSLIT_5;
                break;
            case LONGSLIT_6:
                fpu = GmosNFpu.LONGSLIT_6;
                break;
            case LONGSLIT_7:
                fpu = GmosNFpu.LONGSLIT_7;
                break;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosNBlueprintLongSlit)) return false;
        if (!super.equals(o)) return false;

        GmosNBlueprintLongSlit that = (GmosNBlueprintLongSlit) o;

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
        return "GmosNBlueprintLongSlit{" +
                "fpu=" + fpu +
                "} " + super.toString();
    }

    @Override
    public BlueprintPair toMutable() {
        final GmosNBlueprintChoice choice = new GmosNBlueprintChoice();
        choice.setRegime(getWavelengthRegime());

        final edu.gemini.model.p1.mutable.GmosNBlueprintLongslit blueprint = new edu.gemini.model.p1.mutable.GmosNBlueprintLongslit();
        choice.setLongslit(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setFpu(getFpu());
        blueprint.setFilter(getFilter());
        blueprint.setDisperser(getDisperser());
        blueprint.setAltair(getAltairConfiguration().toAltairChoice());

        return new BlueprintPair(choice, blueprint);
    }

    public GmosNFpu getFpu() {
        return fpu;
    }

    public void setFpu(final GmosNFpu fpu) {
        this.fpu = fpu;
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        final GmosSBlueprintLongSlit blueprint = new GmosSBlueprintLongSlit(this);

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
