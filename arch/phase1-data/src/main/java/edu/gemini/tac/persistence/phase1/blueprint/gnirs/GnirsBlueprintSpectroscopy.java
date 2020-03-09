package edu.gemini.tac.persistence.phase1.blueprint.gnirs;

import edu.gemini.model.p1.mutable.GnirsBlueprintChoice;
import edu.gemini.model.p1.mutable.GnirsCrossDisperser;
import edu.gemini.model.p1.mutable.GnirsDisperser;
import edu.gemini.model.p1.mutable.GnirsFpu;
import edu.gemini.model.p1.mutable.GnirsCentralWavelength;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintPair;
import edu.gemini.tac.persistence.phase1.blueprint.flamingos2.Flamingos2BlueprintLongslit;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@DiscriminatorValue("GnirsBlueprintSpectroscopy")
public class GnirsBlueprintSpectroscopy extends GnirsBlueprintBase
{
    @Enumerated(EnumType.STRING)
    @Column(name = "disperser")
    protected GnirsDisperser disperser = GnirsDisperser.D_10;

    @Enumerated(EnumType.STRING)
    @Column(name = "cross_disperser")
    protected GnirsCrossDisperser crossDisperser = GnirsCrossDisperser.NO;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "fpu")
    protected GnirsFpu fpu = GnirsFpu.SW_1;

    @Column(name = "central_wavelength_range")
    @Enumerated(EnumType.STRING)
    protected GnirsCentralWavelength centralWavelengthRange = GnirsCentralWavelength.LT_25;

    public GnirsBlueprintSpectroscopy() { setInstrument(Instrument.GNIRS); }

    public GnirsBlueprintSpectroscopy(Flamingos2BlueprintLongslit blueprint) {
        super(blueprint.getBlueprintId(), blueprint.getName(), blueprint.getObservations());
    }

    @Override
    public BlueprintPair toMutable() {
        final GnirsBlueprintChoice choice = new GnirsBlueprintChoice();
        final edu.gemini.model.p1.mutable.GnirsBlueprintSpectroscopy mBlueprint = new edu.gemini.model.p1.mutable.GnirsBlueprintSpectroscopy();
        choice.setSpectroscopy(mBlueprint);

        mBlueprint.setAltair(getAltairConfiguration().toAltairChoice());
        mBlueprint.setDisperser(getDisperser());
        mBlueprint.setCrossDisperser(getCrossDisperser());
        mBlueprint.setFpu(getFpu());
        mBlueprint.setId(getBlueprintId());
        mBlueprint.setName(getName());
        mBlueprint.setPixelScale(getPixelScale());
        mBlueprint.setCentralWavelength(centralWavelengthRange);

        return new BlueprintPair(choice, mBlueprint);
    }

    public GnirsBlueprintSpectroscopy(final edu.gemini.model.p1.mutable.GnirsBlueprintSpectroscopy mBlueprint) {
        super(mBlueprint.getId(), mBlueprint.getName(), mBlueprint.getAltair(), mBlueprint.getPixelScale(), new HashSet<edu.gemini.tac.persistence.phase1.Observation>());
        setDisperser(mBlueprint.getDisperser());
        setCrossDisperser(mBlueprint.getCrossDisperser());
        setFpu(mBlueprint.getFpu());
        setCentralWavelengthRange(mBlueprint.getCentralWavelength());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GnirsBlueprintSpectroscopy)) return false;
        if (!super.equals(o)) return false;

        GnirsBlueprintSpectroscopy that = (GnirsBlueprintSpectroscopy) o;

        if (crossDisperser != that.crossDisperser) return false;
        if (disperser != that.disperser) return false;
        if (fpu != that.fpu) return false;
        if (centralWavelengthRange != that.centralWavelengthRange) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (disperser != null ? disperser.hashCode() : 0);
        result = 31 * result + (crossDisperser != null ? crossDisperser.hashCode() : 0);
        result = 31 * result + (fpu != null ? fpu.hashCode() : 0);
        result = 31 * result + (centralWavelengthRange != null ? centralWavelengthRange.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GnirsBlueprintSpectroscopy{" +
                "disperser=" + disperser +
                ", crossDisperser=" + crossDisperser +
                ", fpu=" + fpu +
                ", centralWavelengthRange=" + centralWavelengthRange +
                '}';
    }

    public GnirsDisperser getDisperser() {
        return disperser;
    }

    public void setDisperser(GnirsDisperser disperser) {
        this.disperser = disperser;
    }

    public GnirsCrossDisperser getCrossDisperser() {
        return crossDisperser;
    }

    public void setCrossDisperser(GnirsCrossDisperser crossDisperser) {
        this.crossDisperser = crossDisperser;
    }

    public GnirsFpu getFpu() {
        return fpu;
    }

    public void setFpu(GnirsFpu fpu) {
        this.fpu = fpu;
    }

    public GnirsCentralWavelength getCentralWavelengthRange() {
        return centralWavelengthRange;
    }

    public void setCentralWavelengthRange(GnirsCentralWavelength centralWavelengthRange) {
        this.centralWavelengthRange = centralWavelengthRange;
    }

    public BlueprintBase getComplementaryInstrumentBlueprint(){
        return new Flamingos2BlueprintLongslit(this);
    }

    @Override
    public Map<String, Set<String>> getResourcesByCategory(){
        Map<String, Set<String>> resources = super.getResourcesByCategory();

        resources.put("disperser", new HashSet<String>());
        resources.put("crossDisperser", new HashSet<String>());
        resources.put("fpu", new HashSet<String>());
        resources.put("centralWavelengthRange", new HashSet<String>());

        resources.get("disperser").add(disperser.value());
        resources.get("crossDisperser").add(crossDisperser.value());
        resources.get("fpu").add(fpu.value());
        resources.get("centralWavelengthRange").add(centralWavelengthRange.value());

        return resources;

    }

    @Override
    public String getDisplayFocalPlaneUnit() {
        return getFpu().value();
    }

    @Override
    public String getDisplayDisperser() {
        return getDisperser().value() + ", " + getCrossDisperser().value();
    }

    @Override
    public String getDisplayFilter() {
        return DISPLAY_NOT_APPLICABLE;
    }

    @Override
    public String getDisplayOther() {
        return DISPLAY_NOT_APPLICABLE;
    }
}
