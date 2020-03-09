package edu.gemini.tac.persistence.phase1.blueprint.nici;

import edu.gemini.model.p1.mutable.NiciBlueprintChoice;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.niri.NiriBlueprint;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.HashSet;

@Entity
@DiscriminatorValue("NiciBlueprintStandard")
public class NiciBlueprintStandard extends NiciBlueprintBase {
    public NiciBlueprintStandard() { setInstrument(Instrument.NICI); }
    public NiciBlueprintStandard(edu.gemini.model.p1.mutable.NiciBlueprintStandard mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), mBlueprint.getDichroic(), mBlueprint.getRedFilter(), mBlueprint.getBlueFilter(), new HashSet<Observation>());
    }

    @Override
    public BlueprintPair toMutable() {
        final NiciBlueprintChoice choice = new NiciBlueprintChoice();

        final edu.gemini.model.p1.mutable.NiciBlueprintStandard blueprint = new edu.gemini.model.p1.mutable.NiciBlueprintStandard();
        choice.setStandard(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setDichroic(getDichroic());
        blueprint.getRedFilter().addAll(getRedFilters());
        blueprint.getBlueFilter().addAll(getBlueFilters());

        return new BlueprintPair(choice, blueprint);
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new NiriBlueprint(this);
    }

    @Override
    public String getDisplayOther() {
        return getDichroic().value();
    }
}
