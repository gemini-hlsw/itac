package edu.gemini.tac.persistence.queues;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class QueueTest extends FastHibernateFixture {
    private static final Logger LOGGER = Logger.getLogger(QueueTest.class.getName());

    @Resource(name = "sessionFactory")
    SessionFactory sessionFactory;

    @Test
    public void canAddProposal(){
        Queue q = new Queue();

        final Proposal p = getProposal(0);

        Banding banding = new Banding(q, p, ScienceBand.BAND_ONE);
        q.addBanding(banding);
        Assert.assertTrue(q.getBandings().contains(banding));
    }

    @Test
    @Ignore
    public void canGenerateProgramIds(){
        
        final Session session = sessionFactory.openSession();
        try {
            final Queue q = getQueue(session, 0);
            final Proposal p = getProposal(session, 0);

            Banding banding = new Banding(q, p, ScienceBand.BAND_ONE);
            q.addBanding(banding);

            q.programIds(true);

            Assert.assertEquals("GN-2010B-Q-1", banding.getProgramId());

            Proposal p2 = getProposal(session, 1);
            Banding b2 = new Banding(q, p2, ScienceBand.BAND_ONE);
            q.addBanding(b2);

            q.programIds(true);
            Assert.assertEquals("GN-2010B-Q-2", banding.getProgramId());
            Assert.assertEquals("GN-2010B-Q-1", b2.getProgramId());
            q.programIds(false);
            Assert.assertEquals("GN-2010B-Q-1", banding.getProgramId());
            Assert.assertEquals("GN-2010B-Q-2", b2.getProgramId());
        } finally {
            session.close();
        }
    }
}