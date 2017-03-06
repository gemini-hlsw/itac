package edu.gemini.tac.persistence.phase1.blueprint.nifs;

import edu.gemini.model.p1.mutable.NifsBlueprintChoice;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.flamingos2.Flamingos2BlueprintMos;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("NifsBlueprint")
public class NifsBlueprint extends NifsBlueprintBase {
    protected NifsBlueprint() { setInstrument(Instrument.NIFS); }

    public NifsBlueprint(final edu.gemini.model.p1.mutable.NifsBlueprint mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), mBlueprint.getDisperser(), new HashSet<Observation>());
    }

    @Override
    public BlueprintPair toMutable() {
        final NifsBlueprintChoice choice = new NifsBlueprintChoice();
        final edu.gemini.model.p1.mutable.NifsBlueprint mBlueprint = new edu.gemini.model.p1.mutable.NifsBlueprint();
        choice.setNonAo(mBlueprint);

        mBlueprint.setDisperser(getDisperser());
        mBlueprint.setId(getBlueprintId());
        mBlueprint.setName(getName());

        return new BlueprintPair(choice, mBlueprint);
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new Flamingos2BlueprintMos(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        Map<String, Set<String>> resources = super.getResourcesByCategory();

        return resources;
    }

    @Override
    public String getDisplayCamera() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayFocalPlaneUnit() {
                return DISPLAY_NOT_APPLICABLE;
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
