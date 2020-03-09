package edu.gemini.tac.persistence.emails;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Date;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class EmailTest {
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(EmailTest.class.getName());

    @Resource(name = "sessionFactory")
    SessionFactory sessionFactory;

    @Test
    public void createEmail() {
        final Session session = sessionFactory.openSession();
        try {

            Email email = new Email();

            email.setAddress("foo@bar.edu");
            email.setComment("comment");
            email.setContent("Ipsum lorum...");
            email.setSentTimestamp(new Date());
            email.setErrorTimestamp(new Date());

            session.save(email);
            session.flush();


        } finally {
            session.close();
        }
    }

}
