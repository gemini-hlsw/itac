package edu.gemini.tac.persistence.bug;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import junit.framework.Assert;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Set;

/**
 * Reference support was broken for site qualities as well in a very simliar manner to targets.  This test
 * confirms that the fixed linkage remains.  I also noticed (finally) that resource linkage is broken in
 * a simliar manner and fixed that.
 * <p/>
 * TODO: Right now, I haven't built test fixtures for observation specific site qualities, only
 * for observation lists.  I have reasonable confidence that the fix will work for both.
 *
 * @author ddawson
 */
public class NoSiteQualitiesInObservationsOrObservationLists extends HibernateFixture {
    @Resource(name = "sessionFactory")
    SessionFactory sessionFactory;

    @Before
    public void before() {
        super.teardown();
        proposals = getProposals();

    }

    @Test
    public void testObservationListRetrieval() {
        for (Proposal p : proposals) {
            testObservationListRetrieval(p);
        }
    }

    private void testObservationListRetrieval(Proposal p) {
        final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
        final Set<Observation> observations = phaseIProposal.getObservations();
        Assert.assertNotNull(observations);
        for (Observation o : observations) {
            testObservationListRetrieval(o);
        }
    }

    private void testObservationListRetrieval(Observation o) {
        final Condition condition = o.getCondition();
        Assert.assertNotNull(condition);
        Assert.assertNotNull(condition.getCloudCover());
        Assert.assertNotNull(condition.getImageQuality());
        Assert.assertNotNull(condition.getSkyBackground());
    }
}
