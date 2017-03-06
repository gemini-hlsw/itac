package edu.gemini.tac.persistence.bug;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.Observation;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * With the new phase1-data (2012), the old need for this test has become obsolete. Now just tests the mundane
 * "every observation has conditions" condition
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/data-applicationContext.xml"})
public class NoSiteQualitiesInObservationsOrObservationListsTest extends HibernateFixture {
	@Resource(name = "sessionFactory")
	SessionFactory sessionFactory;

    @Before
    public void before(){
        super.teardown();
        super.before();
        proposals = getProposals();
    }

	@Test
	public void testObservationListRetrieval() {
        final Proposal p = proposals.get(0);
        final Set<Observation> observations = p.getPhaseIProposal().getObservations();

        assertNotNull(observations);

        for(Observation o : observations){
            testObservationListRetrieval(o);
        }
    }

    private void testObservationListRetrieval(Observation o){
        final Condition condition = o.getCondition();
        assertNotNull(condition);
        assertNotNull(condition.getCloudCover());
        assertNotNull(condition.getSkyBackground());
        assertNotNull(condition.getImageQuality());
        assertNotNull(condition.getWaterVapor());
	}
}
