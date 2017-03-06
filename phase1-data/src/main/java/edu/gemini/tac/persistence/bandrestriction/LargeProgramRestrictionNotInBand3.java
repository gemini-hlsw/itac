package edu.gemini.tac.persistence.bandrestriction;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("LargeProgramRestrictionNotInBand3")
public class LargeProgramRestrictionNotInBand3 extends BandRestrictionRule {
}
