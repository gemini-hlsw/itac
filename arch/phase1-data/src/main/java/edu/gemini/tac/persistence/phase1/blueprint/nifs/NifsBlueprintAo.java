package edu.gemini.tac.persistence.phase1.blueprint.nifs;

import edu.gemini.model.p1.mutable.AltairChoice;
import edu.gemini.model.p1.mutable.NifsBlueprintChoice;
import edu.gemini.model.p1.mutable.NifsOccultingDisk;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.altair.AltairConfiguration;
import org.apache.commons.lang.NotImplementedException;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("NifsBlueprintAo")
public class NifsBlueprintAo extends NifsBlueprintBase {
    @Enumerated(EnumType.STRING)
    @Column(name = "altair_configuration")
    protected AltairConfiguration altairConfiguration = AltairConfiguration.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "occulting_disk")
    protected NifsOccultingDisk occultingDisk;

    protected NifsBlueprintAo() {}

    public NifsBlueprintAo(final edu.gemini.model.p1.mutable.NifsBlueprintAo mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), mBlueprint.getDisperser(), new HashSet<Observation>());

        setOccultingDisk(mBlueprint.getOccultingDisk());
        setAltairConfiguration(AltairConfiguration.fromAltairChoice(mBlueprint.getAltair()));
    }

    @Override
    public BlueprintPair toMutable() {
        final NifsBlueprintChoice choice = new NifsBlueprintChoice();
        final edu.gemini.model.p1.mutable.NifsBlueprintAo mBlueprint = new edu.gemini.model.p1.mutable.NifsBlueprintAo();
        choice.setAo(mBlueprint);

        final AltairChoice altairChoice = altairConfiguration.toAltairChoice();
        mBlueprint.setAltair(altairChoice);
        mBlueprint.setDisperser(getDisperser());
        mBlueprint.setId(getBlueprintId());
        mBlueprint.setName(getName());
        mBlueprint.setOccultingDisk(getOccultingDisk());

        return new BlueprintPair(choice, mBlueprint);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NifsBlueprintAo)) return false;
        if (!super.equals(o)) return false;

        NifsBlueprintAo that = (NifsBlueprintAo) o;

        if (altairConfiguration != that.altairConfiguration) return false;
        if (occultingDisk != that.occultingDisk) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (altairConfiguration != null ? altairConfiguration.hashCode() : 0);
        result = 31 * result + (occultingDisk != null ? occultingDisk.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NifsBlueprintAo{" +
                "altairConfiguration=" + altairConfiguration +
                ", occultingDisk=" + occultingDisk +
                '}';
    }

    @Override
    public edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase getComplementaryInstrumentBlueprint() {
        throw new NotImplementedException();
    }

    public AltairConfiguration getAltairConfiguration() {
        return altairConfiguration;
    }

    public void setAltairConfiguration(AltairConfiguration altairConfiguration) {
        this.altairConfiguration = altairConfiguration;
    }

    public NifsOccultingDisk getOccultingDisk() {
        return occultingDisk;
    }

    public void setOccultingDisk(NifsOccultingDisk occultingDisk) {
        this.occultingDisk = occultingDisk;
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        Map<String, Set<String>> resources = super.getResourcesByCategory();

        resources.put("altairConfiguration", new HashSet<String>());
        resources.put("occultingDisk", new HashSet<String>());

        resources.get("altairConfiguration").add(altairConfiguration.value());
        resources.get("occultingDisk").add(occultingDisk.value());

        return resources;
    }

    @Override
    public String getDisplayCamera() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayFocalPlaneUnit() {
        return getOccultingDisk().value();
    }

    @Override
    public String getDisplayFilter() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayAdaptiveOptics() {
        return getAltairConfiguration().getDisplayValue();
    }

    @Override
    public String getDisplayOther() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public boolean hasLgs(){
        switch (altairConfiguration) {
            case LGS_WITH_PWFS1:
            case LGS_WITHOUT_PWFS1:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean hasAltair() {
        if (altairConfiguration == AltairConfiguration.NONE) {
            return false;
        } else {
            return true;
        }
    }
}
