package edu.gemini.tac.service;

import edu.gemini.tac.persistence.bandrestriction.BandRestrictionRule;

import java.util.List;

public interface IBandRestrictionService {
	List<BandRestrictionRule> getAllBandRestrictions();
}
