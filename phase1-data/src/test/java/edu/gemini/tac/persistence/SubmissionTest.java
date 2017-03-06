package edu.gemini.tac.persistence;

import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class SubmissionTest extends HibernateFixture {
    private static final Logger LOGGER = Logger.getLogger(ProposalTest.class.getName());
    @Resource
    SessionFactory sessionFactory;

    @Before
    public void before() {
        super.teardown();
        super.before();
        sessionFactory = getSessionFactory();
        proposals = getProposals();
    }

    @Test
    public void canCreateNgoSubmission(){
        Assert.assertTrue(ngoSubmissions.size() > 0);
    }

    @Test
    public void canCreateExchangeSubmission(){
        Assert.assertTrue(exchangeSubmissions.size() > 0);
    }

    @Test
    public void canCreateLargeProgramSubmissions(){
        Assert.assertTrue(largeProgramSubmissions.size() > 0);
    }

}

