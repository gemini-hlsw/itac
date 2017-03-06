package edu.gemini.tac.persistence.bandrestriction;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("RapidTooRestrictionInBandOne")
public class RapidTooRestrictionInBandOne extends BandRestrictionRule {
}
