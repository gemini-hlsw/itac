package edu.gemini.tac.service.configuration;

import edu.gemini.tac.persistence.bin.BinConfiguration;

import java.util.List;

/**
 * Facade for retreiving and updating bin configurations.
 * 
 * @author ddawson
 *
 */
public interface IBinConfigurationsService {
	/**
	 * @return all bin configurations known about in the system.
	 */
	List<BinConfiguration> getAllBinConfigurations();
	
	/**
	 * Retrieve a single condition set by its id.
	 * 
	 * @param name the id of the bin configuration, for example, "250"
	 * @return the condition set if found
	 */
	BinConfiguration getBinConfigurationById(final Long id);

	/**
	 * Retrieve a single bin configuration by its name.
	 * 
	 * @param name the descriptive name of the bin configuration, for example, "Bin Configuration 2"
	 * @return the bin configuration if found
	 */
	BinConfiguration getBinConfigurationByName(final String name);
	/**
	 * Saves or updates a bin configuration
	 * 
	 * @param conditionSet a (presumably modified) bin configurationto be persisted back to the underlying data store.
	 */
	void updateBinConfiguration(final BinConfiguration binConfiguration);

}
