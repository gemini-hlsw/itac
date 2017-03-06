package edu.gemini.tac.persistence.fixtures.builders;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.queues.ScienceBand;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class QueueBuilderTest extends FastHibernateFixture.Basic {

    @Resource(name = "sessionFactory")
    protected SessionFactory sessionFactory;

    @Test
    @Transactional
    public void canCreateMinimalQueue() {
        new QueueBuilder(getCommittee(0)).
                addBanding(createProposal(), ScienceBand.BAND_ONE).
                addBanding(createProposal(), ScienceBand.BAND_ONE).
                create(sessionFactory.getCurrentSession());
        sessionFactory.getCurrentSession().flush();   // force errors!
    }
    
    private Proposal createProposal() {
        Proposal p = new QueueProposalBuilder().
                setCommittee(getCommittee(0)).
                setPartner(getPartner("US")).
                addObservation("Vega", "00:00:00.000", "00:00:00.000", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        sessionFactory.getCurrentSession().flush();   // force errors!
        return p;
    }
}
