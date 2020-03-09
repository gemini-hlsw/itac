package edu.gemini.tac.persistence;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.CoInvestigator;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.PrincipalInvestigator;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class ContactTest extends FastHibernateFixture.WithProposalsLoadOnce {
    private static final Logger LOGGER = Logger.getLogger(ContactTest.class);

    /**
     * Re-purposed to test new hierarchy.  Contacts begone.  Investigators forever.
     */
    @Test
    @Transactional
    public void retrievesContact() {
        for (Proposal p : getProposals()) {
            Proposal  mergedP = (Proposal) getSessionFactory().getCurrentSession().merge(p);
            retrievesContact(mergedP.getPhaseIProposal());
        }
    }

    public void retrievesContact(PhaseIProposal p) {
        final PrincipalInvestigator pi = p.getInvestigators().getPi();
        final Long piId = pi.getId();
        //Skip if pi's ID is null, which is possible in derivedPhaseIProposal of joint proposal -- since derived PIs are transient
        if(piId != null){
        final PrincipalInvestigator newPi =
                (PrincipalInvestigator) doHibernateQueryTakingId("from PrincipalInvestigator where id = :id", piId);
        assertNotNull(newPi);
        assertEquals(newPi.getId(), piId);
        assertNotNull(newPi.getEmail());
        assertFalse(newPi.getEmail().length() == 0);
        }

        final Iterator<CoInvestigator> cois = p.getInvestigators().getCoi().iterator();
        final boolean someCois = cois.hasNext();

        if (someCois) {
            final CoInvestigator coInvestigator = cois.next();

            final Long coInvestigatorId = coInvestigator.getId();

            final CoInvestigator newCoi =
                    (CoInvestigator) doHibernateQueryTakingId("from CoInvestigator where id = :id", coInvestigatorId);
            assertNotNull(newCoi);
            assertEquals(coInvestigatorId, newCoi.getId());
        }
    }

}

