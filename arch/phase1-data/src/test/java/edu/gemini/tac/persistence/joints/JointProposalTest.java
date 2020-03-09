package edu.gemini.tac.persistence.joints;

import edu.gemini.model.p1.mutable.InvestigatorStatus;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.phase1.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class
        JointProposalTest extends FastHibernateFixture.WithProposalsLoadOnce {
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(JointProposalTest.class.getName());

    @Test
    @Transactional
    public void createsDot() {
        Proposal p = getProposal(getSessionFactory().getCurrentSession(), 0);
        String s = p.toDot();
        Assert.assertTrue(s.length() > 50);
    }

    @Test
    @Transactional
    public void canGetIdentityValue() {
        Proposal p = getProposal(getSessionFactory().getCurrentSession(), 0);
        Long id = p.getId();
        Assert.assertNotNull(id);
    }

    @Test
    @Transactional
    public void defaultMergerManglesDifferentPIs() {
        LOGGER.log(Level.DEBUG, "defaultMergerManglesDifferentPIs()");
        JointProposal jp = createJointProposal(getSessionFactory().getCurrentSession());
        // make sure proposals have different PIs
        int i = 0;
        List<String> expectedFirst = new ArrayList<String>();
        List<String> expectedLast = new ArrayList<String>();
        List<String> expectedEmail = new ArrayList<String>();
        for (Proposal p : jp.getProposals()) {
            PrincipalInvestigator pi = new PrincipalInvestigator();
            pi.setFirstName("F"+i);
            pi.setLastName("L"+i);
            pi.setEmail("E"+i);
            pi.setStatus(InvestigatorStatus.OTHER);
            p.getPhaseIProposal().getInvestigators().setPi(pi);
            expectedFirst.add(pi.getFirstName());
            expectedLast.add(pi.getLastName());
            expectedEmail.add(pi.getEmail());
            i++;
        }
        // check what we get from joint proposal
        PrincipalInvestigator jpPI = jp.getPhaseIProposal().getInvestigators().getPi();
        Assert.assertEquals(StringUtils.join(expectedFirst, "/"), jpPI.getFirstName());
        Assert.assertEquals(StringUtils.join(expectedLast, "/"),  jpPI.getLastName());
        Assert.assertEquals(StringUtils.join(expectedEmail, ";"), jpPI.getEmail());
    }

    @Test
    @Transactional
    public void defaultMergerIdentifiesSimilarPIs() {
        LOGGER.log(Level.DEBUG, "defaultMergerIdentifiesSimilarPIs()");
        JointProposal jp = createJointProposal(getSessionFactory().getCurrentSession());
        // make sure proposals have same PIs
        for (Proposal p : jp.getProposals()) {
            PrincipalInvestigator pi = new PrincipalInvestigator();
            pi.setFirstName("F");
            pi.setLastName("L");
            pi.setEmail("E");
            pi.setStatus(InvestigatorStatus.OTHER);
            p.getPhaseIProposal().getInvestigators().setPi(pi);
        }
        // check what we get from joint proposal
        PrincipalInvestigator jpPI = jp.getPhaseIProposal().getInvestigators().getPi();
        Assert.assertEquals("F", jpPI.getFirstName());
        Assert.assertEquals("L",  jpPI.getLastName());
        Assert.assertEquals("E", jpPI.getEmail());
    }

    @Test
    @Transactional
    public void defaultMergerCombinesInvestigators() {
        LOGGER.log(Level.DEBUG, "defaultMergeCombinesInvestigators()");
        JointProposal jp = createJointProposal(getSessionFactory().getCurrentSession());

        final Investigators primaryInvestigators = jp.getPrimaryProposal().getPhaseIProposal().getInvestigators();
        final Set<Investigators> secondaryInvestigatorGroups = new HashSet<Investigators>();
        int totalCois = primaryInvestigators.getCoi().size();
        for (Proposal secondaryPropsal : jp.getSecondaryProposals()) {
            final Investigators investigators1 = secondaryPropsal.getPhaseIProposal().getInvestigators();
            totalCois += investigators1.getCoi().size();
            secondaryInvestigatorGroups.add(investigators1);
        }
        Assert.assertTrue(totalCois > 0);

        final Set<CoInvestigator> mergedCois = jp.getPhaseIProposal().getInvestigators().getCoi();
        Assert.assertEquals(totalCois, mergedCois.size());
    }

    @Test
    @Transactional
    public void jointCanBeCreated() throws IOException {
        LOGGER.log(Level.DEBUG, "jointCanBeCreated()");
        Session session = getSessionFactory().getCurrentSession();
        Proposal p0 = getProposal(session, 0);
        Proposal p1 = getProposal(session, 1);
        Assert.assertFalse(p0.isJoint());
        Assert.assertFalse(p1.isJoint());
        Set<Proposal> ps = new HashSet<Proposal>();
        ps.add(p0);
        ps.add(p1);

        JointProposal merged = null;
        merged = new JointProposal();
        merged.setProposals(ps);
        //Set the primary, this will also set committee, partner and issues of joint
        merged.setPrimaryProposal(p0);
        session.save(merged);
        session.flush();  // force errors

        int mId = merged.getId().intValue();
        Assert.assertTrue(mId > 0); //Actually, the test is just whether the previous throws a LIE

        Assert.assertTrue(merged.isJoint());
        Assert.assertTrue(merged.hasComponent(p0));
        Assert.assertTrue(merged.hasComponent(p1));
    }

    @Test
    @Transactional
    public void knowsPrimaryProposal() {
        LOGGER.log(Level.DEBUG, "knowsPrimaryProposal()");
        JointProposal jp = createJointProposal(getSessionFactory().getCurrentSession());
        Proposal p = jp.getPrimaryProposal();
        Assert.assertNotNull(p);
    }

    @Test
    @Transactional
    public void canReturnSecondaryProposals() {
        LOGGER.log(Level.DEBUG, "canReturnSecondaryProposals()");
        JointProposal jp = createJointProposal(getSessionFactory().getCurrentSession());
        Set<Proposal> ps = jp.getProposals();
        Assert.assertNotNull(ps);
        Assert.assertTrue(ps.size() > 0);
    }

    @Test
    @Transactional
    //ITAC-161
    public void canCreateAbbreviations() {
        LOGGER.log(Level.DEBUG, "canCreateAbbreviations()");
        JointProposal jp = createJointProposal(getSessionFactory().getCurrentSession());

        String abbrev = jp.getPartnerAbbreviation();
        if (abbrev.equals("J:US/CA") == false) {
            if (abbrev.equals("J:CA/US") == false) {
                Assert.fail("'" + abbrev + "' is neither of acceptable forms");
            }
        }
    }

    @Test
    @Transactional
    public void canDeleteDuplicatedJointProposal() {
        Session session = getSessionFactory().getCurrentSession();
        JointProposal jp = createJointProposal(session);
        JointProposal dup = duplicateJointProposal(session, jp);
        canDeleteDuplicatedProposal22(session, jp, dup);
    }
    
    private JointProposal createJointProposal(Session session) {
        JointProposal jp = new JointProposal();
        Proposal p0 = getProposalFromPartner(session, "US", 0);
        Proposal p1 = getProposalFromPartner(session, "CA", 0);
        jp.add(p0);
        jp.add(p1);
        session.save(jp);
        session.flush(); // force errors
        return jp;
    }
    
    private JointProposal duplicateJointProposal(Session session, JointProposal src) {
        JointProposal duplicate = (JointProposal) src.duplicate();
        // save all child proposals, no cascade defined in model
        for (Proposal p : duplicate.getProposals()) {
            session.save(p);
        }
        session.save(duplicate);
        session.flush(); // force errors
        return duplicate;
    }

    private void canDeleteDuplicatedProposal22(Session session, Proposal src, Proposal dup) {
        final Proposal dup2 = getProposalById(session, dup.getId());
        dup2.delete(session);
        session.flush(); // force errors

        // re-read duplicate document, shouldn't be here anymore
        final Proposal rereadDup = getProposalById(session, dup.getId());
        assertNull(rereadDup);

        // re-read initial document and make sure nothing has been deleted by cascading deletes...
        final Proposal reread = getProposalById(session, src.getId());
        assertNotNull(reread);
        assertNotNull(reread.getPhaseIProposal());
        assertNotNull(reread.getConditions());
        assertNotNull(reread.getTargets());
        assertNotNull(reread.getBlueprints());
        assertNotNull(reread.getObservations());
        assertEquals(src.getConditions().size(), reread.getConditions().size());
        assertEquals(src.getTargets().size(), reread.getTargets().size());
        assertEquals(src.getBlueprints().size(), reread.getBlueprints().size());
        assertEquals(src.getObservations().size(), reread.getObservations().size());
    }

}
