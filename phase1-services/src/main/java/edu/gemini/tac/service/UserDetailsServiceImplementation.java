package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Person;
import edu.gemini.tac.persistence.security.AuthorityRole;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
* Hibernate impl of security user details service. Finds a Person in the persistence layer and constructs a corresponding
* UserDetails object
*
* @author: lobrien
* @created: 2010-11-04
*/

@Service("userDetailsService")
@Transactional
public class UserDetailsServiceImplementation implements UserDetailsService {
    @Resource(name = "sessionFactory")
    SessionFactory sessionFactory;
    private static final Logger LOGGER = Logger.getLogger(UserDetailsServiceImplementation.class.getName());

    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
        LOGGER.log(Level.DEBUG, "loadUserByName(" + username + ")");
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.createQuery("from Person where name = :username");
        query.setString("username", username);
        Person person = (Person) query.uniqueResult();
        try {
            final DatabaseMetaData dmd = session.connection().getMetaData();
            LOGGER.log(Level.DEBUG, "Database connection is " + dmd.getURL());
        } catch (SQLException se) {
            //swallow it
        }
        if (person != null) {
            LOGGER.log(Level.DEBUG, "Person is " + person.toString());
        } else {
            LOGGER.info("Could not find user " + username);

            throw new UsernameNotFoundException("Could not find user " + username);
        }
        return buildUserFromPerson(person);
    }

    User buildUserFromPerson(Person person) {
        String username = person.getName();
        String password = person.getPassword();
        boolean enabled = person.isEnabled();
        boolean accountNonExpired = person.isEnabled();
        boolean credentialsNonExpired = person.isEnabled();
        boolean accountNonLocked = person.isEnabled();

        Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (AuthorityRole role : person.getAuthorities()) {
            authorities.add(new GrantedAuthorityImpl(role.getRolename()));
        }

        return new User(username, password, enabled,
                accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
    }
}
