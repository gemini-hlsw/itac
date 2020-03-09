package edu.gemini.tac.persistence.phase1.blueprint.nici;

import edu.gemini.model.p1.mutable.*;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("NiciBlueprintCoronagraphic")
public class NiciBlueprintCoronagraphic extends NiciBlueprintBase {
    @Column(name = "fpm")
    @Enumerated(EnumType.STRING)
    protected NiciFpm fpm = NiciFpm.MASK_1;

    public NiciBlueprintCoronagraphic() { setInstrument(Instrument.NICI); }
    public NiciBlueprintCoronagraphic(edu.gemini.model.p1.mutable.NiciBlueprintCoronagraphic mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), mBlueprint.getDichroic(), mBlueprint.getRedFilter(), mBlueprint.getBlueFilter(), new HashSet<edu.gemini.tac.persistence.phase1.Observation>());

        setFpm(mBlueprint.getFpm());
    }

    @Override
    public String toString() {
        return "NiciBlueprintCoronagraphic{" +
                "fpm=" + fpm +
                "} " + super.toString();
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        return new edu.gemini.tac.persistence.phase1.blueprint.niri.NiriBlueprint(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        final Map<String, Set<String>> resources = super.getResourcesByCategory();

        resources.put("fpm", new HashSet<String>());

        resources.get("fpm").add(fpm.value());

        return resources;
    }

    @Override
    public String getDisplayOther() {
        return getDichroic().value() + "," + fpm.value();
    }

    @Override
    public BlueprintPair toMutable() {
        final NiciBlueprintChoice choice = new NiciBlueprintChoice();

        final edu.gemini.model.p1.mutable.NiciBlueprintCoronagraphic blueprint = new edu.gemini.model.p1.mutable.NiciBlueprintCoronagraphic();
        choice.setCoronagraphic(blueprint);

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setDichroic(getDichroic());
        blueprint.setFpm(getFpm());
        blueprint.getRedFilter().addAll(getRedFilters());
        blueprint.getBlueFilter().addAll(getBlueFilters());

        return new BlueprintPair(choice, blueprint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NiciBlueprintCoronagraphic)) return false;
        if (!super.equals(o)) return false;

        NiciBlueprintCoronagraphic that = (NiciBlueprintCoronagraphic) o;

        if (fpm != that.fpm) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (fpm != null ? fpm.hashCode() : 0);
        return result;
    }

    public NiciFpm getFpm() {
        return fpm;
    }

    public void setFpm(NiciFpm fpm) {
        this.fpm = fpm;
    }
}
