package edu.gemini.tac.persistence;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Test PhaseIProposal
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class PhaseIProposalTest extends FastHibernateFixture.WithProposalsLoadOnce {
   private static final Logger LOGGER = Logger.getLogger(PhaseIProposalTest.class.getName());

    @Test
    @Transactional
    public void canRetrieveParent(){
        Proposal p = getProposal(sessionFactory.getCurrentSession(), 0);
        Assert.assertNotNull(p);
        PhaseIProposal p1p = p.getPhaseIProposal();
        Assert.assertNotNull(p1p);
        Proposal andBackAgain = p1p.getParent();
        Assert.assertEquals(p, andBackAgain);
    }

    @Test
    @Transactional
    public void retrievesBandObservationsProperly(){
        Proposal p = getProposal(sessionFactory.getCurrentSession(), 0);
        final Set<Observation> activeObservations = p.getObservations();
        Assert.assertTrue(activeObservations.size() > 0);
        final List<Observation> band1Band2ActiveObservations = p.getPhaseIProposal().getBand1Band2ActiveObservations();
        Assert.assertTrue(band1Band2ActiveObservations.size() == 0);
        final List<Observation> b3Observations = p.getPhaseIProposal().getBand3Observations();
        Assert.assertTrue(b3Observations.size()  > 0);
        Assert.assertEquals(activeObservations.size(), b3Observations.size()  + band1Band2ActiveObservations.size());
    }
}
