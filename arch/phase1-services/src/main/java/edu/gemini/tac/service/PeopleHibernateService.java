package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Person;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service(value="peopleService")
@Transactional
public class PeopleHibernateService implements IPeopleService {
	@Resource(name = "sessionFactory")
	private SessionFactory sessionFactory;

	@Override
	public Person create(final Person person) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public Person findById(final Long personId) {
		final Session session = sessionFactory.getCurrentSession();

		final String queryString = String.format("from Person p where p.id = %d",personId);
		final Query query = session.createQuery(queryString);
		final Person person = (Person) query.uniqueResult();

		return person;
	}

	@Override
	public Person findByName(final String userName) {
		final Session session = sessionFactory.getCurrentSession();

        final Query query = session.getNamedQuery("person.findByName").setParameter("name", userName);
		final Person person = (Person) query.uniqueResult();

		return person;
	}

    @Override
    public Person findMe(final String userName) {
        final Session session = sessionFactory.getCurrentSession();

        final Query query = session.getNamedQuery("person.findWithCommitteesByName").setParameter("name", userName);
        final Person person = (Person) query.uniqueResult();

        return person;
    }

	@Override
	@SuppressWarnings("unchecked")
	public List<Person> getAllPeople() {
        return sessionFactory.getCurrentSession().getNamedQuery("person.getAllPeople").list();
	}

	@Override
	public Person update(Person person) {
        final Session session = sessionFactory.getCurrentSession();
        final Person mergedPerson = (Person) session.merge(person);
        session.update(mergedPerson);
        session.refresh(person);

        return mergedPerson;
	}

/**
     * Uses eager fetch of memberships
     *
     * @param person
     * @return
     */
    @Override
    public List<Committee> getAllCommitteesFor(Person person) {
        final Session session = sessionFactory.getCurrentSession();
        person = (Person) session.merge(person);
        final Set<Committee> committees = person.getCommittees();
        for (Committee c : committees) {
            c.getMembers().size();
            c.getQueues().size();
            c.getProposals().size();
        }

        final ArrayList<Committee> sortMeList = new ArrayList<Committee>(committees);
        Collections.sort(sortMeList, new Comparator<Committee>() {
            @Override
            public int compare(Committee left, Committee right) {
                final int activeDifference = left.getActive().compareTo(right.getActive());
                if (activeDifference != 0 )
                    return -activeDifference;

                return left.getSemester().compareTo(right.getSemester());
            }
        });

        return sortMeList;
    }
}
