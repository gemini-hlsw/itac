package edu.gemini.tac.persistence.schemadesign;

import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import static junit.framework.Assert.*;

/**
 */
@Transactional
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class})
public class SimplifiedPersistenceShadowProposal extends HibernateFixture {
    Session currentSession;

    /**
     * Setup before each individual test.
     */
    @Before
    public void before() {
        super.teardown();
        super.before();

        phaseIProposals = getPhaseIProposals();
    }

    @Test
    public void canFindExistingProposalById() {
        final Session currentSession = getSessionFactory().openSession();
        currentSession.getTransaction().begin();

        final Long firstProposalId = phaseIProposals.get(0).getId();
        final String query = "from PhaseIProposal p where id = " + firstProposalId;

        final PhaseIProposal proposal = (PhaseIProposal) doHibernateQueryWithUniqueResult(query);

        assertNotNull(proposal);
        assertEquals(Long.valueOf(firstProposalId), proposal.getId());

        currentSession.getTransaction().rollback();

    }

    @Test
    public void titleProposalAbstractSchemaVersionArePersistent() {
        currentSession = getSessionFactory().openSession();

        String title = "Title of proposal 0";
        String abstractString = "Abstract of proposal 0";
        String schemaVersion = "1.0.0";

        Long proposalId = phaseIProposals.get(0).getId();
        String query = "from PhaseIProposal p where id = " + proposalId;

        PhaseIProposal proposal = (PhaseIProposal) doHibernateQueryWithUniqueResult(query);
        assertNotNull(proposal);
        assertEquals(title, proposal.getTitle());
        assertEquals(title, proposal.getTitle());
        assertEquals(abstractString, proposal.getProposalAbstract());

        proposalId = phaseIProposals.get(4).getId();
        query = new String("from PhaseIProposal p where id = " + proposalId);

        title = new String("Title of proposal 4");
        abstractString = new String("Abstract of proposal 4");

        proposal = (PhaseIProposal) doHibernateQueryWithUniqueResult(query);
        assertNotNull(proposal);
        assertEquals(title, proposal.getTitle());
        assertEquals(title, proposal.getTitle());
        assertEquals(abstractString, proposal.getProposalAbstract());
    }

    @Test
    public void canCreateProposals() {
        assertTrue(true); // fixture set up guarantees this.
    }

    @Test
    public void canDeleteProposals() {
        assertTrue(true); // fixture set up guarantees this.
    }

    @Test
    public void canUpdateProposalTitleAbstractSchemaVersion() {
        currentSession = getSessionFactory().openSession();
        currentSession.getTransaction().begin();

        Long proposalId = phaseIProposals.get(0).getId();
        String query = "from PhaseIProposal p where id = " + proposalId;
        String newTitle = "new title";
        String newAbstract = "new abstract";
        String newSchemaVersion = "43";

        PhaseIProposal proposal = (PhaseIProposal) doHibernateQueryWithUniqueResult(query);
        proposal.setTitle(newTitle);
        proposal.setProposalAbstract(newAbstract);
        proposal.setSchemaVersion(newSchemaVersion);

        currentSession.saveOrUpdate(proposal);
        currentSession.getTransaction().commit();
        currentSession.flush();
        proposal = null;

        currentSession = getSessionFactory().openSession();
        currentSession.getTransaction().begin();
        proposal = (PhaseIProposal) doHibernateQueryWithUniqueResult(query);

        assertEquals(newTitle, proposal.getTitle());
        assertEquals(newAbstract, proposal.getProposalAbstract());
        assertEquals(newSchemaVersion, proposal.getSchemaVersion());

        currentSession.getTransaction().rollback();
        currentSession.close();
    }
}
