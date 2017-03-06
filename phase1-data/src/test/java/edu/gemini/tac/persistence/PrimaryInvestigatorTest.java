package edu.gemini.tac.persistence;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.builders.QueueProposalBuilder;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.PrincipalInvestigator;
import org.apache.log4j.Logger;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class PrimaryInvestigatorTest extends FastHibernateFixture.WithProposalsLoadOnce {
    private static final Logger LOGGER = Logger.getLogger(PrimaryInvestigatorTest.class);

    Long p1ProposalId;
    String piEmail;

    @Before
    public void before() {
        p1ProposalId = getPhaseIProposal(0).getId();

        final Session session = sessionFactory.openSession();
        try {
            Proposal p = new QueueProposalBuilder().
                    setCommittee(getCommittee(0)).
                    setPartner(getPartner("US")).
                    addObservation("Vega", "00:00:00.000", "00:00:00.000", 1, TimeUnit.HR).
                    create(session);
            piEmail = p.getPhaseIProposal().getInvestigators().getPi().getEmail();
            session.flush();   // force errors!

            p1ProposalId = p.getPhaseIProposal().getId();
        } finally {
            session.close();
        }
    }

    @Test
    public void retrievesPIDirectly() {

        final PhaseIProposal p = (PhaseIProposal) doHibernateQueryTakingId("from PhaseIProposal p join fetch p.investigators pi join fetch pi.pi where p.id = :id", p1ProposalId);
        final Long piId = p.getInvestigators().getPi().getId();
        final PrincipalInvestigator pi = (PrincipalInvestigator) doHibernateQueryTakingId("from PrincipalInvestigator where id = :id", piId);
        assertNotNull(pi);
        assertEquals(piEmail, pi.getEmail());
    }

    /**
     * Not sure what used to be violating decency here, but I've tried to adapt the spirit of the test.
     */
    @Test
    public void violatesAllSenseOfDecency() {
        final PhaseIProposal p = (PhaseIProposal) doHibernateQueryTakingId("from PhaseIProposal p join fetch p.investigators pi join fetch pi.pi where p.id = :id", p1ProposalId);
        final Long piId = p.getInvestigators().getPi().getId();

        Session session = getSessionFactory().openSession();
        try {
            String q1 = "from PrincipalInvestigator pi where pi.id = :id";
            assertTrue(surelyTheseTwoFunctionsShouldBehaveIdentically(q1, false, piId));
            String q2 = "from PrincipalInvestigator pi left outer join fetch pi.phoneNumbers where pi.id = :id";
            assertTrue(surelyTheseTwoFunctionsShouldBehaveIdentically(q2, false, piId));
        } finally {
            session.close();
        }
    }

    public boolean surelyTheseTwoFunctionsShouldBehaveIdentically(String q, boolean shouldThrow, final Long id) {
        PrincipalInvestigator pi = (PrincipalInvestigator) doHibernateQueryTakingId(q,id);
        assertNotNull(pi);
        assertNotNull(pi.getEmail());
        boolean threw = false;
        try {
            String email = pi.getEmail();
            assertEquals(piEmail, email);
        } catch (LazyInitializationException lie) {
            threw = true;
        }
        assertEquals(shouldThrow, threw);
        return true;
    }

    @Test
    public void retrievesWithContactInfo() {
        final PhaseIProposal p = (PhaseIProposal) doHibernateQueryTakingId("from PhaseIProposal p join fetch p.investigators pi join fetch pi.pi where p.id = :id", p1ProposalId);
        final Long piId = p.getInvestigators().getPi().getId();

        PrincipalInvestigator pi = (PrincipalInvestigator)
                doHibernateQueryTakingId("from PrincipalInvestigator pi left join fetch pi.phoneNumbers where pi.id = :id", piId);
        assertNotNull(pi);
        assertNotNull(pi.getEmail());
        assertNotNull(pi.getPhoneNumbers());
        assertEquals(1, pi.getPhoneNumbers().size());
        assertTrue(pi.getPhoneNumbers().iterator().next().contains("888 999-7565"));
        assertNotNull(pi.getInstitutionAddress());
        assertTrue(pi.getInstitutionAddress().getInstitution().contains("Gemini Observatory"));
        assertTrue(pi.getInstitutionAddress().getAddress().contains("670 N. Aohoku Pl"));
        assertTrue(pi.getInstitutionAddress().getCountry().contains("USA"));
    }
}
