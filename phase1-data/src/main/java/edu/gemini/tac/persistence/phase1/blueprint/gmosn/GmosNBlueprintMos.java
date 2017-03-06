package edu.gemini.tac.persistence.phase1.blueprint.gmosn;

import edu.gemini.model.p1.mutable.GmosNBlueprintChoice;
import edu.gemini.model.p1.mutable.GmosNMOSFpu;
import edu.gemini.model.p1.mutable.GmosNWavelengthRegime;
import edu.gemini.model.p1.mutable.GmosSMOSFpu;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gmoss.GmosSBlueprintMos;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("GmosNBlueprintMos")
public class GmosNBlueprintMos
    extends GmosNBlueprintSpectroscopyBase
{
    @Column(name = "preimaging")
    protected Boolean preimaging = Boolean.FALSE;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "fpu")
    protected GmosNMOSFpu fpu = GmosNMOSFpu.LONGSLIT_2;

    protected GmosNBlueprintMos() { { setInstrument(Instrument.GMOS_N);} }

    public GmosNBlueprintMos(final edu.gemini.model.p1.mutable.GmosNBlueprintMos mBlueprint, final GmosNWavelengthRegime regime) {
        super(mBlueprint.getId(), mBlueprint.getName(), regime, mBlueprint.getDisperser(), mBlueprint.getFilter(), mBlueprint.getAltair());

        setFpu(mBlueprint.getFpu());
        setPreimaging(mBlueprint.isPreimaging());
    }

    public GmosNBlueprintMos(GmosSBlueprintMos blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());

        setPreimaging(blueprint.isPreimaging());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GmosNBlueprintMos)) return false;
        if (!super.equals(o)) return false;

        GmosNBlueprintMos that = (GmosNBlueprintMos) o;

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
        return "GmosNBlueprintMos{" +
                "preimaging=" + preimaging +
                "fpu=" + fpu +
                "} " + super.toString();
    }

    @Override
    public BlueprintPair toMutable() {
        final GmosNBlueprintChoice choice = new GmosNBlueprintChoice();
        choice.setRegime(getWavelengthRegime());

        final edu.gemini.model.p1.mutable.GmosNBlueprintMos blueprint = new edu.gemini.model.p1.mutable.GmosNBlueprintMos();
        choice.setMos(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setDisperser(getDisperser());
        blueprint.setPreimaging(isPreimaging());
        blueprint.setFilter(getFilter());
        blueprint.setFpu(getFpu());
        blueprint.setAltair(getAltairConfiguration().toAltairChoice());

        return new BlueprintPair(choice, blueprint);
    }

    public GmosNMOSFpu getFpu() {
        return fpu;
    }

    public void setFpu(GmosNMOSFpu fpu) {
        this.fpu = fpu;
    }

    public boolean isPreimaging() {
        return preimaging;
    }

    public void setPreimaging(boolean preimaging) {
        this.preimaging = preimaging;
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        return new GmosSBlueprintMos(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        final Map<String, Set<String>> resources = super.getResourcesByCategory();

        resources.put("preimaging", new HashSet<String>());
        resources.get("preimaging").add(preimaging ? "preimaging" : "");
        resources.put("fpu", new HashSet<String>());
        resources.get("fpu").add(fpu.name());
        resources.put("altairConfiguration", new HashSet<String>());
        resources.get("altairConfiguration").add(altairConfiguration.value());

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
