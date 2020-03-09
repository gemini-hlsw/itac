package edu.gemini.tac.persistence;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import junit.framework.Assert;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class PersonTest extends FastHibernateFixture.BasicLoadOnce {

    @Test
    public void peopleExist() {
        final Session session = sessionFactory.openSession();
        try {
            final Query query = session.createQuery("from Person where enabled='true'");
            List enabledPeople = query.list();
            assertTrue(enabledPeople.size() > 0);
        } finally {
            session.close();
        }
    }

    @Test
    public void adminExists() {
        final Session session = sessionFactory.openSession();

        try {
            final Query query = session.createQuery("from Person where name = 'admin'");
            Person admin = (Person) query.uniqueResult();
            assertNotNull(admin);
            final Query query2 = session.createQuery("from Person where name='admin'");
            Person a2 = (Person) query2.uniqueResult();
            assertEquals(admin, a2);
        } finally {
            session.close();
        }
    }

    @Test
    public void personToStringFormatsLovely() {
        final Session session = sessionFactory.openSession();
        try {
            final Query query = session.createQuery("from Person where name = 'admin'");
            Person admin = (Person) query.uniqueResult();
            String s = admin.toString();
            assertTrue(s.contains("name=admin,password=password,enabled=true"));
        } finally {
            session.close();
        }
    }

    @Test
    public void generallyHasAPartner() {
        final Session session = sessionFactory.openSession();
        try {
            final Query query = session.createQuery("from Person where name = 'committee_member'");
            Person person = (Person) query.uniqueResult();
            Partner partner = person.getPartner();
            Assert.assertNotNull(partner);
        } finally {
            session.close();
        }
    }

    @Test
    public void nameAndPasswordDefineEquality(){
        Person p1 = new Person();
        p1.setName("foo");
        p1.setPassword("bar");
        //Differences
        p1.setEnabled(true);
        p1.setId(1L);
        p1.setPartner(new Partner());

        Person p2 = new Person();
        p2.setName("foo");
        p2.setPassword("bar");
        //Differences
        p2.setEnabled(false);
        p2.setId(2L);
        p2.setPartner(new Partner());

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
