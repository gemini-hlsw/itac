package edu.gemini.tac.persistence.bug;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.GuideStar;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.Target;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Set;

/**
 * The queue engine was misbehaving when we were integrating the web application with it.  Well, ok
 * it wasn't misbehaving, it was rather reporting that targets weren't being attached to observations.
 * This test is being used to drive the fix and then make sure we don't regress.
 *
 * @author ddawson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/data-applicationContext.xml"})
public class NoTargetsInObservationsTest extends HibernateFixture {
    private static final Logger LOGGER = Logger.getLogger(NoTargetsInObservationsTest.class.getName());

    @Before
    public void before(){
        super.teardown();
        super.before();
        proposals = getProposals();
    }

    void testObservationRetrieval(Proposal p){
        final Set<Observation> observations = p.getPhaseIProposal().getObservations();
        for(Observation o : observations){
            final Target target = o.getTarget();
            Assert.assertNotNull(target);
            final Set<GuideStar> guideStars = o.getGuideStars();
            Assert.assertNotNull(guideStars);
        }
    }
    
    @Test
    public void testObservationRetrieval() {
        for(Proposal p : proposals){
            testObservationRetrieval(p);
        }
    }
}
