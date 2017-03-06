package edu.gemini.tac.persistence.phase1.blueprint.gmoss;

import edu.gemini.model.p1.mutable.GmosSBlueprintChoice;
import edu.gemini.model.p1.mutable.GmosSMOSFpu;
import edu.gemini.model.p1.mutable.GmosSWavelengthRegime;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gmosn.GmosNBlueprintMos;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("GmosSBlueprintMos")
public class GmosSBlueprintMos
    extends GmosSBlueprintSpectroscopyBase
{
    @Column(name = "preimaging")
    protected boolean preimaging = Boolean.FALSE;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "fpu")
    protected GmosSMOSFpu fpu = GmosSMOSFpu.LONGSLIT_1;

    public GmosSBlueprintMos() { setInstrument(Instrument.GMOS_S); }

    public GmosSBlueprintMos(edu.gemini.model.p1.mutable.GmosSBlueprintMos mBlueprint, GmosSWavelengthRegime regime) {
        super(mBlueprint.getId(), mBlueprint.getName(), regime, mBlueprint.getDisperser(), mBlueprint.getFilter());

        setPreimaging(mBlueprint.isPreimaging());
        setFpu(mBlueprint.getFpu());
        setNodAndShuffle(Boolean.FALSE);
    }

    public GmosSBlueprintMos(final GmosNBlueprintMos blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());
        setPreimaging(blueprint.isPreimaging());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosSBlueprintMos)) return false;
        if (!super.equals(o)) return false;

        GmosSBlueprintMos that = (GmosSBlueprintMos) o;

        if (preimaging != that.preimaging) return false;
        if (!fpu.equals(that.fpu)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (preimaging ? 1 : 0);
        result = 31 * result + (fpu != null ? fpu.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GmosSBlueprintMos{" +
                "preimaging=" + preimaging +
                "fpu=" + fpu +
                "} " + super.toString();
    }

    @Override
    public BlueprintPair toMutable() {
        final GmosSBlueprintChoice choice = new GmosSBlueprintChoice();
        choice.setRegime(getWavelengthRegime());

        final edu.gemini.model.p1.mutable.GmosSBlueprintMos blueprint = new edu.gemini.model.p1.mutable.GmosSBlueprintMos();
        choice.setMos(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setDisperser(getDisperser());
        blueprint.setPreimaging(isPreimaging());
        blueprint.setFilter(getFilter());
        blueprint.setFpu(getFpu());

        return new BlueprintPair(choice, blueprint);
    }

    public GmosSMOSFpu getFpu() {
        return fpu;
    }

    public void setFpu(GmosSMOSFpu fpu) {
        this.fpu = fpu;
    }

    public boolean isPreimaging() {
        return preimaging;
    }

    public void setPreimaging(boolean preimaging) {
        this.preimaging = preimaging;
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        return new GmosNBlueprintMos(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        final Map<String, Set<String>> resources = super.getResourcesByCategory();

        resources.put("preimaging", new HashSet<String>());
        resources.get("preimaging").add(preimaging ? "preimaging" : "");

        resources.put("fpu", new HashSet<String>());
        resources.get("fpu").add(fpu.name());

        return resources;
    }

    @Override
    public boolean isMOS() {
        return true;
    }

    @Override
    public String getDisplayFocalPlaneUnit() {
        return getFpu().value();
    }

    @Override
    public String getDisplayOther() {
        return super.getDisplayOther() + ((preimaging) ? " preimaging" : "");
    }

}
