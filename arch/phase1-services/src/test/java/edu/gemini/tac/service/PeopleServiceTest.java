package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Person;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class PeopleServiceTest extends FastHibernateFixture.BasicLoadOnce {
    @Resource(name = "peopleService")
    IPeopleService service;

    @Test
    public void find() {
        final List<Person> allPeople = service.getAllPeople();
        assertNotNull(allPeople);
        assertEquals(11, allPeople.size());
    }

    @Test
    public void findById() {
        final Person some = service.getAllPeople().get(0);
        final Person person = service.findById(some.getId());

        assertNotNull(person);
        assertEquals(some.getName(), person.getName());
        assertEquals(some.getPassword(), person.getPassword());
    }

    @Test
    public void findByName() {
        final Person some = service.getAllPeople().get(0);
        final Person person = service.findByName(some.getName());
        assertNotNull(person);
        assertEquals(some.getId(), person.getId());
    }

    @Test
    public void canRetrieveCommitteesForMember() {
        final Person p = service.getAllPeople().get(0);
        List<Committee> committees = service.getAllCommitteesFor(p);
        assertTrue(committees.size() > 0);
    }
}
