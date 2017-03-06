package edu.gemini.tac.persistence.restrictedbin;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("LgsObservationsRestrictedBin")
public class LgsObservationsRestrictedBin extends RestrictedBin {

    @Override
    public RestrictedBin copy(Integer value) {
        LgsObservationsRestrictedBin restriction = new LgsObservationsRestrictedBin();
        restriction.setId(null);
        restriction.setValue(value);
        restriction.setDescription(this.getDescription());
        restriction.setUnits(this.getUnits());
        return restriction;
    }
}
