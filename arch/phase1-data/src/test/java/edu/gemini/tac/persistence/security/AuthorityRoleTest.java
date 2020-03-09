package edu.gemini.tac.persistence.security;

import edu.gemini.tac.persistence.Person;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

/**
 * Tests of the AuthorityRole object
 * <p/>
 * Author: lobrien
 * Created: Nov 4, 2010
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/data-applicationContext.xml"})
public class AuthorityRoleTest extends FastHibernateFixture {

    @Resource(name = "sessionFactory")
    protected SessionFactory sessionFactory;

    @Test
    public void classExists(){
        AuthorityRole ar = new AuthorityRole();
        assertNotNull(ar);
    }

	@Test
	public void rolesExist() {
        final Session session = sessionFactory.openSession();
        try{
        final Query query = session.createQuery("from AuthorityRole");
        List roles = query.list();
        assert(roles.size() > 0);
        }finally{
            session.close();
        }
	}

    @Test
    public void adminExists() {
        final Session session = sessionFactory.openSession();
        try {
            final Query query2 = session.createQuery("from AuthorityRole where rolename='ROLE_ADMIN'");
            AuthorityRole a2 = (AuthorityRole) query2.uniqueResult();
            assertNotNull(a2);
        } finally {
            session.close();
        }
    }

    @Test
    public void adminUserHasAdminRole() {
        final Session session = sessionFactory.openSession();
        try {
            final Query query2 = session.createQuery("from AuthorityRole where rolename='ROLE_ADMIN'");
            AuthorityRole adminRole = (AuthorityRole) query2.uniqueResult();
            final Query query = session.createQuery("from Person where name='admin'");
            Person aPerson = (Person) query.uniqueResult();
            Set<AuthorityRole> authorities = aPerson.getAuthorities();
            assert(authorities.contains(adminRole));
            assert(aPerson.isAdmin());
        } finally {
            session.close();
        }
    }

    @Test
    public void normalUserDoesNotHaveAdminRole() {
        final Session session = sessionFactory.openSession();
        try {
            final Query query2 = session.createQuery("from AuthorityRole where rolename='ROLE_ADMIN'");
            AuthorityRole adminRole = (AuthorityRole) query2.uniqueResult();
            final Query query = session.createQuery("from Person where name='user'");
            Person aPerson = (Person) query.uniqueResult();
            Set<AuthorityRole> authorities = aPerson.getAuthorities();
            assert(! authorities.contains(adminRole));
            assert(!aPerson.isAdmin());
        } finally {
            session.close();
        }
    }
}

