package edu.gemini.tac.persistence.bug;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The queue engine was misbehaving when we were integrating the web application with it.  Well, ok
 * it wasn't misbehaving, it was rather reporting that targets weren't being attached to observations.
 * This test is being used to drive the fix and then make sure we don't regress.
 * 
 * @author ddawson
 *
 */
public class NoTargetsInObservations  extends HibernateFixture {
	@Resource(name = "sessionFactory")
	SessionFactory sessionFactory;

	@Test
	public void testObservationRetrieval() {
		final Session session = sessionFactory.openSession();
		final Transaction transaction = session.beginTransaction();

        final Proposal proposal = proposals.get(0);
        assertNotNull(proposal);
        final PhaseIProposal phaseIProposal = proposal.getPhaseIProposal();
        assertNotNull(phaseIProposal);
        final Set<Observation> observations = phaseIProposal.getObservations();
        assertNotNull(observations);
        assertTrue(observations.size() > 0);

        for (Observation o : observations) {
            assertNotNull(o.getTarget());
        }

		transaction.commit();
		session.close();
	}
}
