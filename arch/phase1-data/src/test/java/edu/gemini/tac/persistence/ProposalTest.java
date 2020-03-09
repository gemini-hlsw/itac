package edu.gemini.tac.persistence;

import edu.gemini.tac.exchange.ProposalExporterImpl;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.EphemerisElement;
import edu.gemini.tac.persistence.phase1.NonsiderealTarget;
import edu.gemini.tac.persistence.phase1.Target;
import edu.gemini.tac.persistence.phase1.proposal.ClassicalProposal;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.proposal.QueueProposal;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class ProposalTest extends HibernateFixture {
    private static final Logger LOGGER = Logger.getLogger(ProposalTest.class.getName());
    @Resource
    SessionFactory sessionFactory;

    @Before
    public void before() {
        super.teardown();
        super.before();
        sessionFactory = getSessionFactory();
        proposals = getProposals();
        saveOrUpdateAll(authorityRoles);
        saveSemesterCommitteesProposalsPeople();
         for (Proposal p : proposals) {
            for (Target t : p.getPhaseIProposal().getTargets()) {
                if (t instanceof NonsiderealTarget) {
                    NonsiderealTarget nst = (NonsiderealTarget) t;
                    Assert.assertEquals(p.getPhaseIProposal(), nst.getPhaseIProposal());
                    final List<EphemerisElement> ephemeris = nst.getEphemeris();
                    Assert.assertNotNull(ephemeris);
                    Assert.assertTrue(ephemeris.size() > 0);
                    for(EphemerisElement ee : ephemeris){
                        Assert.assertNotNull(ee.getCoordinates());
                        Assert.assertNotNull(ee.getMagnitude());
                    }
                }
            }
        }

        partners = getPartners();
        groupProposalsByPartner();
    }

    private void canDeleteDuplicatedProposal(Proposal initial, Proposal dup) {
        //Validate that we've got some data worth checking (if add fields here, add to final re-read below)
        assertNotNull(initial);
        final PhaseIProposal initProposal = initial.getPhaseIProposal();
        assertNotNull(initProposal);
        final List<Condition> conditions = initProposal.getConditions();
        assertNotNull(conditions);

        //Make sure the dup is saved (so we can test retrieval)
        final Session session = sessionFactory.openSession();
        final Transaction tx = session.beginTransaction();
        try {
            //TODO: Confirm that we want to do these types of save explicitly rather than cascade
            if (dup instanceof JointProposal) {
                for (Proposal p : ((JointProposal) dup).getProposals()) {
                    session.saveOrUpdate(p);
                }
            }
            session.saveOrUpdate(dup);
            tx.commit();
        } finally {
            session.close();
        }

        // delete the duplicate
        final Session session2 = sessionFactory.openSession();
        final Transaction tx2 = session2.beginTransaction();
        try {
            final Proposal dup2 = getProposalById(session2, dup.getId());
            dup2.delete(session2);
            tx2.commit();
        } catch (RuntimeException e) {
            tx2.rollback();
            throw e;
        } finally {
            session2.close();
        }

        // re-read initial document and make sure nothing has been deleted...
        final Session session3 = sessionFactory.openSession();
        final Transaction tx3 = session3.beginTransaction();
        try {
            final Proposal reread = getProposalById(session3, initial.getId());
            assertNotNull(reread);
            assertNotNull(reread.getPhaseIProposal());
            assertNotNull(reread.getPhaseIProposal().getConditions());
            tx3.commit();
        } catch (RuntimeException e) {
            tx3.rollback();
            throw e;
        } finally {
            session3.close();
        }

    }

    @Test
    public void belongsToSite() {
        final Session session = sessionFactory.openSession();

        try {
            saveOrUpdateAll(sites);

            final Site siteNorth = (Site) session.getNamedQuery("site.findSiteByDisplayName").setString("displayName", "North").uniqueResult();
            final Site siteSouth = (Site) session.getNamedQuery("site.findSiteByDisplayName").setString("displayName", "South").uniqueResult();
            assertNotNull(siteNorth);
            assertNotNull(siteSouth);

            final Proposal proposal = proposals.get(0);
            assertNotNull(proposal);
            assertTrue("proposal 0 does not belong to South", !proposal.belongsToSite(siteSouth));
            assertTrue("proposal 0 belongs to North", proposal.belongsToSite(siteNorth));
        } finally {
            session.close();
        }
    }

    @Test
    public void canDeleteDuplicatedProposal() {
        final Proposal p = proposals.get(0);
        final Proposal dup = p.duplicate();
        canDeleteDuplicatedProposal(p, dup);
    }

    @Test
    public void accessIsLimitedToPeopleFromPartnerCountry() {
        LOGGER.log(Level.DEBUG, "accessIsLimitedToPeopleFromPartnerCountry()");
        final Session session = sessionFactory.openSession();

        try {
            //final Query query = session.createQuery("from Proposal p join fetch p.partner where p.id = " + proposals.get(0).getId());
            //final Proposal proposal = (Proposal) query.uniqueResult();
            final Proposal proposal = getProposalFromPartner("CA", 0);
            assertNotNull(proposal);
            LOGGER.log(Level.DEBUG, "proposal is not null");
            Query query1 = session.createQuery("from Person p left join fetch p.partner where p.name = 'committee_member'");
            assertNotNull(query1);
            LOGGER.log(Level.DEBUG, "Query is created");
            final Person member = (Person) query1.uniqueResult();
            assertNotNull(member);
            LOGGER.log(Level.DEBUG, "Got the member");
            assertTrue(proposal.isFullyVisibleTo(member));
            LOGGER.log(Level.DEBUG, "Confirmed visibility");
            final Person non_member = (Person) session.createQuery("from Person where name = 'Devin'").uniqueResult();
            assertNotNull(non_member);
            LOGGER.log(Level.DEBUG, "Got the non-member");
            assertTrue(!proposal.isFullyVisibleTo(non_member));
            LOGGER.log(Level.DEBUG, "test passed");
        } finally {
            session.close();
        }
    }

    @Test
    public void canDuplicate() {
        LOGGER.log(Level.DEBUG, "canDuplicate()");
        Proposal p = proposals.get(0);
        Proposal dup = p.duplicate();
        Assert.assertEquals(p.getPartner(), dup.getPartner());
        Assert.assertEquals(p.getPhaseIProposal().getTitle(), dup.getPhaseIProposal().getTitle());
        //...could be extended into a deep value comparison, but that seems like its own test...

        //Things that are not the same
        Assert.assertNotSame(p.getItac(), dup.getItac());
        Assert.assertNotSame(p.getId(), dup.getId());
    }

    @Test
    public void canRetrieveSite() {
        LOGGER.log(Level.DEBUG, "canRetrieveSite()");
        Proposal p = proposals.get(0);
        Site site = p.getSite();
        Assert.assertEquals(Site.NORTH, site);
    }

    @Test
    public void proposalsExist() {
        LOGGER.log(Level.DEBUG, "proposalsExist()");
        Assert.assertTrue(proposals.size() > 0);
    }

    @Test
    public void proposalsHaveValidSubmissions() {
        LOGGER.log(Level.DEBUG, "proposalsHaveValidSubmissions()");
        for (Proposal p : proposals) {
            final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
            assertNotNull(p);
            if (phaseIProposal instanceof ClassicalProposal || phaseIProposal instanceof QueueProposal) {
                assertNotNull(phaseIProposal.getSubmissions());
                assert(phaseIProposal.getSubmissions().size() > 0);
            }
        }
    }

    @Test
    public void proposalsCanBeConvertedToXml() throws Exception {
        Proposal p = proposals.get(0);
        Session s = sessionFactory.openSession();
        try {
            final ProposalExporterImpl proposalExporter = new ProposalExporterImpl(s);
            final byte[] xml = proposalExporter.getAsXml(p);
            Assert.assertNotNull(xml);
        } finally {
            s.close();
        }
    }
}
