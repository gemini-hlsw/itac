package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Person;

import java.util.List;

public interface IPeopleService {
    List<Person> getAllPeople();

    Person findById(final Long id);

    Person findByName(final String userName);

    Person findMe(final String userName);

    Person create(final Person person);

    Person update(final Person person);

    /**
     * Retrieves the committees to which the passed-in Person belongs
     *
     * @param person
     * @return a List<Committee> corresponding to the Membership property of the Committee
     */
    List<Committee> getAllCommitteesFor(final Person person);
}
