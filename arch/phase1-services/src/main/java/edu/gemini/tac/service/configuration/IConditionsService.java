package edu.gemini.tac.service.configuration;

import edu.gemini.tac.persistence.condition.ConditionSet;

import java.util.List;

/**
 * Service facade for managing, retrieving and updating conditions.
 * 
 * @author ddawson
 *
 */
public interface IConditionsService {
	/**
	 * Retrieve a single condition set by its id.
	 * 
	 * @param name the descriptive name of the conditon set, for example, "75"
	 * @return the condition set if found
	 */
	ConditionSet getConditionSetById(final Long id);

	/**
	 * Retrieve a single condition set by its name.
	 * 
	 * @param name the descriptive name of the conditon set, for example, "Semester B"
	 * @return the condition set if found
	 */
	ConditionSet getConditionSetByName(final String name);
	/**
	 * Retrieves every condition set in the system.
	 * 
	 * @return every condition set in the system
	 */
	List<ConditionSet> getAllConditionSets();
	/**
	 * Saves or updates an condition set.
	 * 
	 * @param conditionSet a (presumably modified) condition set to be persisted back to the underlying data store.
	 */
	void updateConditionSet(final ConditionSet conditionSet);

    /**
     * Creates a new condition set based on a previous one with updated percentages attached to the conditions.
     *
     * @param conditionSet - condition set containing existing conditions to be cloned
     * @param name - name of new condition set
     * @param conditionIds - ordered array of ids that will be matched up with the percentages
     * @param percentages - ordered array of percentages that correspond to the id of the conditions belonging
     *                          to the copied conditionSet
     */
    public void copyConditionSetAndUpdatePercentages(final ConditionSet conditionSet, final String name, Long[] conditionIds, Integer[] percentages);
}
