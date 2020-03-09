package edu.gemini.tac.persistence.phase1.blueprint.flamingos2;

import edu.gemini.model.p1.mutable.Flamingos2BlueprintChoice;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.gnirs.GnirsBlueprintImaging;
import edu.gemini.tac.persistence.phase1.blueprint.niri.NiriBlueprint;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("Flamingos2BlueprintImaging")
public class Flamingos2BlueprintImaging extends Flamingos2BlueprintBase {

    @SuppressWarnings("unused")
    public Flamingos2BlueprintImaging() { setInstrument(Instrument.FLAMINGOS2); }

    public Flamingos2BlueprintImaging(final edu.gemini.model.p1.mutable.Flamingos2BlueprintImaging mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), mBlueprint.getFilter());
    }

    public Flamingos2BlueprintImaging(GnirsBlueprintImaging blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());
    }

    public Flamingos2BlueprintImaging(NiriBlueprint blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());
    }

    @Override
    public BlueprintPair toMutable() {
        final Flamingos2BlueprintChoice choice = new Flamingos2BlueprintChoice();
        final edu.gemini.model.p1.mutable.Flamingos2BlueprintImaging blueprint = new edu.gemini.model.p1.mutable.Flamingos2BlueprintImaging();
        choice.setImaging(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.getFilter().addAll(filters);

        return new BlueprintPair(choice, blueprint);
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new NiriBlueprint(this);
    }

    @Override
    public boolean isMOS() {
        return false;
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
    public String getDisplayDisperser() {
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
