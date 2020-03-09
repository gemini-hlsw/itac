package edu.gemini.tac.persistence.phase1.blueprint.exchange;

import edu.gemini.model.p1.mutable.SubaruInstrument;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;

import javax.persistence.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("SubaruBlueprint")
public class SubaruBlueprint extends BlueprintBase {
    @Column(name = "exchange_instrument")
    @Enumerated(EnumType.STRING)
    protected SubaruInstrument subaruInstrument = SubaruInstrument.COMICS;

    @Column(name = "custom_name")
    protected String customName = null;

    public SubaruBlueprint() { setInstrument(Instrument.SUBARU);}

    public SubaruBlueprint(final edu.gemini.model.p1.mutable.SubaruBlueprint mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), Instrument.SUBARU);
        setSubaruInstrument(mBlueprint.getInstrument());
        setCustomName(mBlueprint.getCustomName());
    }

    public SubaruInstrument getSubaruInstrument() {
        return subaruInstrument;
    }

    public void setSubaruInstrument(SubaruInstrument subaruInstrument) {
        this.subaruInstrument = subaruInstrument;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    @Override
    public BlueprintPair toMutable() {
        final edu.gemini.model.p1.mutable.SubaruBlueprint blueprint = new edu.gemini.model.p1.mutable.SubaruBlueprint();

        blueprint.setId(getBlueprintId());
        blueprint.setName(getName());
        blueprint.setInstrument(getSubaruInstrument());
        blueprint.setCustomName(getCustomName());

        return new BlueprintPair(blueprint, blueprint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubaruBlueprint)) return false;
        if (!super.equals(o)) return false;

        SubaruBlueprint that = (SubaruBlueprint) o;

        if (subaruInstrument != that.subaruInstrument) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (subaruInstrument != null ? subaruInstrument.hashCode() : 0);
        result = 31 * result + (customName!= null ? customName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SubaruBlueprint{" +
                "subaruInstrument=" + subaruInstrument +
                "} " + super.toString();
    }

    @Override
    public BlueprintBase getComplementaryInstrumentBlueprint() {
        throw new RuntimeException("Switching sites has no meaning for Keck blueprints.");
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory() {
        final Map<String, Set<String>> resources = new HashMap<String, Set<String>>();

        resources.put("instrument", new HashSet<String>());
        resources.get("instrument").add(subaruInstrument.value());
        if (getCustomName() != null) {
            resources.put("Visitor Name", new HashSet<String>());
            resources.get("Visitor Name").add(getCustomName());
        }

        return resources;
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
    public String getDisplayFilter() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        return DISPLAY_NOT_APPLICABLE;
    }
    
    @Override
    public String getDisplayOther() {
        return subaruInstrument.value();
    }

}
