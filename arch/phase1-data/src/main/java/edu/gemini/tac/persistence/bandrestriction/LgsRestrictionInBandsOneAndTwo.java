package edu.gemini.tac.persistence.bandrestriction;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("LgsRestrictionInBandsOneAndTwo")
public class LgsRestrictionInBandsOneAndTwo extends BandRestrictionRule {
}
